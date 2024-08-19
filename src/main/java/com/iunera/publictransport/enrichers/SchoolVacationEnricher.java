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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParseException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.threeten.extra.Interval;

public class SchoolVacationEnricher implements EnrichmentComputer {

  private static DateTimeFormatter datetimeformatterDay =
      DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.GERMANY);
  private ZoneId germanyzone = ZoneId.of("Europe/Berlin");
  private Locale locale;
  private String subregion;

  private List<Interval> vacations = new ArrayList<>();

  private ObjectMapper om = new ObjectMapper();

  /**
   * https://ferien-api.de/ maybe replace with https://holidayapi.com/countries in the future Fills
   *
   * @param locale of the holidays
   * @param subregion the subregion in a country - e.g. bw for baden Württemberg in germany
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonParseException
   */
  public SchoolVacationEnricher(Locale locale, String subregion)
      throws JsonParseException, JsonMappingException, IOException {
    this.locale = locale;
    this.subregion = subregion;

    for (int year = 2017; year < 2022; year++) {

      URL url =
          new URL("https://ferien-api.de/api/v1/holidays/" + subregion.toUpperCase() + "/" + year);
      URLConnection connection = url.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder sbuilder = new StringBuilder();
      String aux = "";
      while ((aux = in.readLine()) != null) {
        sbuilder.append(aux);
      }

      SchoolVacationDefinitionFerienAPI[] vacations =
          om.readValue(sbuilder.toString(), SchoolVacationDefinitionFerienAPI[].class);

      for (SchoolVacationDefinitionFerienAPI vacation : vacations) {
        this.vacations.add(Interval.of(date(vacation.start), date(vacation.end)));
      }
    }
  }

  private Instant date(String date) {
    date = date.substring(0, 10);
    return LocalDate.parse(date, datetimeformatterDay)
        .atStartOfDay()
        .atZone(germanyzone)
        .toInstant();
  }

  @Override
  public LineRideDetails map(LineRideDetails value) throws Exception {
    boolean isvacation = false;
    Instant currentDate = value.time_OperationDate.atStartOfDay().atZone(germanyzone).toInstant();
    for (Interval vacation : this.vacations) {
      if (vacation.contains(currentDate)) {
        isvacation = true;
        break;
      }
    }
    if (isvacation) {
      value.dayMeta_isSchoolVacation = true;
    }
    return value;
  }
}
