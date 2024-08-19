package com.iunera.publictransport.enrichers;

/*-
 * #%L
 * iu-ridedetails-enrichment
 * %%
 * Copyright (C) 2024 Tim Frey, Christian Schmitt
 * %%
 * Licensed under the OPEN COMPENSATION TOKEN LICENSE (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * <https://github.com/open-compensation-token-license/license/blob/main/LICENSE.md>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @octl.sid: 1b6f7a5d-8dcf-44f1-b03a-77af04433496
 * #L%
 */

import com.google.common.collect.Lists;
import com.iunera.publictransport.ridedetails.EStopType;
import com.iunera.publictransport.ridedetails.LineRideDetails;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.util.Collector;

/**
 * This class computes stateful route details. The state of the last stop is carried to the next
 * stop and this allows to compute KPIs like the current occupation from entrances and exists. It
 * assumes that the values are partition that the stops of a route follow in the logical order how
 * the vehicle drives. Throws an {@link IllegalArgumentException} when there is a violation of the
 * grouping that different lines are in one instance of this class
 */
public class ComputeRouteStateKPIs
    implements GroupReduceFunction<LineRideDetails, LineRideDetails> {

  public ComputeRouteStateKPIs() {}

  @Override
  public void reduce(Iterable<LineRideDetails> values, Collector<LineRideDetails> out)
      throws Exception {
    Double currentOccupation = 0.0;
    LineRideDetails lastvalue = null;

    List<LineRideDetails> valuelist = Lists.newArrayList(values);
    Collections.sort(
        valuelist,
        new Comparator<LineRideDetails>() {

          @Override
          public int compare(LineRideDetails value1, LineRideDetails value2) {
            Instant v1 = Instant.MAX;
            Instant v2 = Instant.MAX;
            if (value1.time_departurePlan != null) v1 = value1.time_departurePlan;
            else if (value1.time_departureReal != null) v1 = value1.time_departureReal;
            if (value2.time_departurePlan != null) v2 = value2.time_departurePlan;
            else if (value2.time_departureReal != null) v2 = value2.time_departureReal;

            return v1.compareTo(v2);
          }
        });

    // all the stops of the line subsequently stopping at each stop
    for (LineRideDetails value : valuelist) {
      if (lastvalue != null && !lastvalue.line_name.equals(value.line_name)) {
        throw new IllegalArgumentException("partition key violation");
      }
      try {

        value.i_occupationArrival = currentOccupation;

        // if it is the beginning stop of a route "empty" the remaining passengers that
        // are there from counting errors
        if (value.stop_type != null && value.stop_type.equals(EStopType.STARTEND)) {
          currentOccupation = value.i_boarding;
        } else {

          currentOccupation = value.i_boarding + currentOccupation - value.i_exits;
        }

        if (currentOccupation < 0) {
          currentOccupation = 0.0;
        }

        value.i_occupationDeparture = currentOccupation;

        currentOccupation = value.i_occupationDeparture;

        if (lastvalue != null) {
          Instant lastdeparture = lastvalue.time_departureReal;
          if (lastdeparture == null) lastdeparture = lastvalue.time_departurePlan;

          Instant arrival = value.time_arrivalReal;
          if (arrival == null) arrival = value.time_arrivalPlan;
          Long duration = null;
          if (arrival != null) {
            duration = arrival.getEpochSecond() - lastdeparture.getEpochSecond();
          }

          lastvalue.i_timeTillNextStop = duration;
          value.i_timeFromLastStop = duration;

          // link the stops and the KPIS to next and prior stop
          value.origin_stop_positionNaturalKey = lastvalue.stop_geoKeys[0];

          lastvalue.destination_stop_positionNaturalKey = value.stop_geoKeys[0];

          value.origin_stop_label = lastvalue.stop_label;

          lastvalue.destination_stop_label = value.stop_label;

          // the previous station details
          value.i_origin_entries = lastvalue.i_boarding;
          value.i_origin_exists = lastvalue.i_exits;
          value.i_origin_retainers = lastvalue.i_retainingGuests;

          lastvalue.i_destination_entries = value.i_boarding;
          lastvalue.i_destination_exists = value.i_exits;
          lastvalue.i_destination_retainers = value.i_retainingGuests;
        }

        lastvalue = value;

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    ;
    // we need to do this here, because when we do it before, we cannot manipulate the last value in
    // the partition
    valuelist.forEach(value -> out.collect(value));
    // partition is finished

  }
}
