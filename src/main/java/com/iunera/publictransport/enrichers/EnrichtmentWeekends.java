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
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** TODO this does not work for all weekends */
public class EnrichtmentWeekends implements EnrichmentComputer {

  private List<DayOfWeek> weekenddays;
  private Locale locale;
  private String subregion;
  /** @param subregion the subregion that might have different weekends */
  public EnrichtmentWeekends(Locale locale, String subregion) {
    this.locale = locale;
    this.subregion = subregion;
    weekenddays = Arrays.asList(EDayGroup.getWeekendDays(locale));
  }

  @Override
  public LineRideDetails map(LineRideDetails value) throws Exception {
    value.dayMeta_weekday = value.time_OperationDate.getDayOfWeek();
    // flinks replication might be null - or not?
    if (weekenddays == null) weekenddays = Arrays.asList(EDayGroup.getWeekendDays(locale));
    if (weekenddays.contains(value.dayMeta_weekday)) {
      value.dayMeta_daygroup = EDayGroup.WEEKEND;
    } else {
      value.dayMeta_daygroup = EDayGroup.WEEKDAY;
    }

    return value;
  }
}
