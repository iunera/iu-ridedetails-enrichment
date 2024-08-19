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

import com.iunera.publictransport.domain.EDayGroup;
import com.iunera.publictransport.ridedetails.LineRideDetails;

public class ConsolidationEnricher implements EnrichmentComputer {

  @Override
  public LineRideDetails map(LineRideDetails value) throws Exception {
    value.dayMeta_areSchoolsOpen =
        !(value.dayMeta_isHoliday
            || (value.dayMeta_isSchoolVacation != null && value.dayMeta_isSchoolVacation)
            || value.dayMeta_daygroup.equals(EDayGroup.WEEKEND));
    return value;
  }
}
