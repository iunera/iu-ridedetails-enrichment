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

import com.iunera.publictransport.domain.TransportProductDTO;
import com.iunera.publictransport.keys.RideKeysGeneration;
import com.iunera.publictransport.ridedetails.LineRideDetails;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class generates a natural key for an event such as a stop that is comparable to other stops
 * near the same geoposition. The fuzzyfication thereby can be specified
 */
public class RepetitiveEventNaturalKeyEnricher implements EnrichmentComputer {

  private static Logger logger = LoggerFactory.getLogger(RepetitiveEventNaturalKeyEnricher.class);

  private ZoneId zone;

  /** @param locale of the public transport */
  public RepetitiveEventNaturalKeyEnricher(ZoneId zone) {
    this.zone = zone;
  }

  /**
   * Contributes dayMeta_dailyRideKey
   *
   * @param value the {@link LineRideDetails} to be enriched
   */
  @Override
  public LineRideDetails map(LineRideDetails value) throws Exception {
    Instant time = value.time_departurePlan;

    if (time == null) {
      time = value.time_departureReal;
    }

    if (time == null) {

      logger.error("no planned or real departure time filled");
      return value;
    }

    LocalTime localtime = time.atZone(this.zone).toLocalTime();

    // TODO:
    // a string of the destination station whereas here we know the direction by the number 1 or 2 -
    // this needs to be fixed to the specific directions
    value.dayMeta_dailyRideStopNearKeys =
        RideKeysGeneration.getNearestTimeBucketsKeys(
            RideKeysGeneration.CoordinateFunctionStop,
            value.longitude,
            value.latitude,
            3,
            localtime,
            value.line_label,
            value.line_transportProduct,
            "UNKNOWN",
            5);

    value.dayMeta_dailyRideStopKey =
        RideKeysGeneration.getRideDayKeyForStop(
            value.longitude,
            value.latitude,
            localtime,
            value.line_label,
            value.line_transportProduct);
    // todo check the planned longitude/latitude here ---
    // add the natural key for the stop
    Double longitude = value.stop_planned_longitude;
    Double latitude = value.stop_planned_latitude;
    if (longitude == null || latitude == null) {
      longitude = value.longitude;
      latitude = value.latitude;
    }
    value.stop_geoKeys =
        new String[] {
          RideKeysGeneration.getKeyForStop(
              longitude, latitude, TransportProductDTO.BUS, RideKeysGeneration.directionUNKNOWN)
        };
    return value;
  }
}
