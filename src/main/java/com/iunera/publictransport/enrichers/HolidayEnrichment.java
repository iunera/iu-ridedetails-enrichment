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
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameter;
import de.jollyday.ManagerParameters;
import java.util.Locale;

public class HolidayEnrichment implements EnrichmentComputer {
  private Locale locale;
  private String subregion;

  /**
   * @param locale of the holidays
   * @param subregion the subregion in a country - e.g. bw for baden Würtemberg in germany
   */
  public HolidayEnrichment(Locale locale, String subregion) {
    this.locale = locale;
    this.subregion = subregion;
    // instance = ;

  }

  private static ManagerParameter params = ManagerParameters.create(Locale.GERMANY);
  private static HolidayManager hm = HolidayManager.getInstance(params);

  @Override
  public LineRideDetails map(LineRideDetails value) throws Exception {
    value.dayMeta_isHoliday = hm.isHoliday(value.time_OperationDate, this.subregion);
    return value;
  }
}
