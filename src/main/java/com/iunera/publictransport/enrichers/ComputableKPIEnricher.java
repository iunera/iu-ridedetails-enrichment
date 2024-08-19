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

import com.iunera.publictransport.ridedetails.EDelayPerception;
import com.iunera.publictransport.ridedetails.LineRideDetails;

public class ComputableKPIEnricher implements EnrichmentComputer {

  @Override
  public LineRideDetails map(LineRideDetails value) throws Exception {
    value.i_absPassengerChange = (value.i_boarding + value.i_exits);

    value.i_absPassengerGrowth = (value.i_boarding - value.i_exits);
    value.i_retainingGuests = value.i_occupationArrival - value.i_exits;

    if (value.time_departureReal != null && value.time_arrivalReal != null)
      value.i_totalStopDuration =
          (value.time_departureReal.getEpochSecond() - value.time_arrivalReal.getEpochSecond());

    if (value.time_arrivalPlan != null && value.time_departureReal != null)
      value.i_depatureStopDuration =
          (value.time_departureReal.getEpochSecond() - value.time_arrivalPlan.getEpochSecond());

    if (value.time_departurePlan != null && value.time_departureReal != null)
      value.i_departureDelay =
          (value.time_departureReal.getEpochSecond() - value.time_departurePlan.getEpochSecond());

    if (value.time_arrivalReal != null && value.time_arrivalPlan != null)
      value.i_arrivalDelay =
          (value.time_arrivalReal.getEpochSecond() - value.time_arrivalPlan.getEpochSecond());

    if (value.i_departureDelay != null) {
      value.perception_departureDelay = EDelayPerception.getDelayPerception(value.i_departureDelay);
    }

    if (value.i_arrivalDelay != null) {
      value.perception_arrivalDelay = EDelayPerception.getDelayPerception(value.i_arrivalDelay);
    }

    if (value.i_boarding != null && value.i_totalStopDuration != null)
      if (value.i_boarding != null && value.i_boarding > 0) {
        value.i_entranceSpeed = value.i_totalStopDuration / (Double) value.i_boarding;
      }

    if (value.i_exits != null && value.i_totalStopDuration != null)
      if (value.i_exits != null && value.i_exits > 0) {
        value.i_exitSpeed = value.i_totalStopDuration / (Double) value.i_exits;
      }

    return value;
  }
}
