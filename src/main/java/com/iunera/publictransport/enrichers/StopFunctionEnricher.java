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

import com.iunera.publictransport.ridedetails.LineRideDetails;

public class StopFunctionEnricher implements EnrichmentComputer {

  @Override
  public LineRideDetails map(LineRideDetails value) throws Exception {
    // determine the stop type of available
    if (value.stop_label != null) {
      if (value.stop_label.toLowerCase().contains("bahnhof")) value.stop_function = "trainstation";
      if (value.stop_label.toLowerCase().contains("hauptbahnhof")
          || value.stop_label.toLowerCase().contains("hbf")) value.stop_function = "mainstation";
      if (value.stop_label.toLowerCase().contains("schule")) value.stop_function = "school";
      if (value.stop_label.toLowerCase().contains("universität"))
        value.stop_function = "university";
    }
    return value;
  }
}
