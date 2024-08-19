package com.iunera.publictransport;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iunera.publictransport.domain.EDayGroup;
import com.iunera.publictransport.domain.TransportProductDTO;
import com.iunera.publictransport.domain.trip.EntryExitActivity;
import com.iunera.publictransport.domain.trip.FilteredFlattenedTripStops;
import com.iunera.publictransport.domain.trip.TripWaypoint;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer.Semantic;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnrichmentJob {

  static boolean local = true;

  static String kafkaBroker = "localhost:9092";
  static String inputTopic = "iu-fahrbar-prod-with-lines";
  static String groupId = "iu-fahrbar-prod-reduce-mgelectronics-groupid-v1";

  static String outputTopic = "iu-fahrbar-prod-enrich-ingrestready";

  static ZoneId timeZone = ZoneId.of("Europe/Berlin");

  private static Logger logger = LoggerFactory.getLogger(EnrichmentJob.class);

  /** Assumptions for this job: The events are in each partition are in order. */
  public static void main(String[] args) throws Exception {

    final ParameterTool params = ParameterTool.fromArgs(args);

    if (params.has("inputTopic")) {
      inputTopic = params.get("inputTopic");
    }

    if (params.has("timeZone")) {
      timeZone = ZoneId.of(params.get("timeZone"));
    }

    if (params.has("outputTopic")) {
      outputTopic = params.get("outputTopic");
    }

    if (params.has("kafkaBroker")) {
      kafkaBroker = params.get("kafkaBroker");
    }

    if (params.has("groupId")) {
      groupId = params.get("groupId");
    }

    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    env.getConfig().setGlobalJobParameters(params);

    // Kafka consumer - reading from Kafka
    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", kafkaBroker);
    properties.setProperty("group.id", groupId);

    FlinkKafkaConsumer<String> myConsumer =
        new FlinkKafkaConsumer<>(inputTopic, new SimpleStringSchema(), properties);

    myConsumer.setStartFromEarliest();

    DataStream<String> stream = env.addSource(myConsumer);

    // associate the watermarks
    WatermarkStrategy<TripWaypoint> rideEventWMStrategy =
        WatermarkStrategy.<TripWaypoint>forBoundedOutOfOrderness(Duration.ofMillis(10 * 30))
            .withIdleness(Duration.ofMinutes(1))
            .withTimestampAssigner(
                (ev, timestamp) -> {
                  return ev.time.toEpochMilli();
                });

    // now we read the ordered file - this is changed to kafka
    DataStream<TripWaypoint> sortedMgFileEventStream =
        stream.map(
            s -> {
              ObjectMapper om = new ObjectMapper();
              om.registerModule(new JavaTimeModule());
              return om.readValue(s, TripWaypoint.class);
            });

    // transforming Ride events to Line Ride details
    SingleOutputStreamOperator<FilteredFlattenedTripStops> rideDetails =
        sortedMgFileEventStream
            .filter(line -> line.stop_scheduleStopDetails != null)
            .map(new LineNaturalKeyEnricher())
            // transform for ingestion
            .map(
                line -> {
                  FilteredFlattenedTripStops ride = new FilteredFlattenedTripStops();
                  ride.latitude = line.geo_latitude;
                  ride.longitude = line.geo_longitude;
                  if (line.stop_scheduleStopDetails != null)
                    ride.stop_id = line.stop_scheduleStopDetails.IFOPT;
                  ride.stop_label = line.stop_name;
                  ride.route_label = line.routeName;
                  ride.routeDetails = line.routeDetails;
                  ride.time_departureReal = line.time;
                  ride.time_OperationDate = line.time_operationDate;
                  ride.vehicle_id = line.vehicle_uniqueId();
                  if (line.doorEntryExitActivity != null) {
                    for (Map.Entry<String, EntryExitActivity> set :
                        line.doorEntryExitActivity.entrySet()) {
                      ride.i_boarding = set.getValue().boarding;
                      ride.i_exits = set.getValue().exiting;
                    }
                  }
                  ride.route_name = line.routeName;
                  ride.route_label = line.routeName;
                  ride.vehicle_operator = line.meta_provider;
                  ride.vehicle_licencePlate = line.vehicle_licensePlate;
                  ride.meta_dataOriginOrganization = line.meta_provider;

                  ride.line_transportProduct = TransportProductDTO.BUS;

                  ride.dayMeta_daygroup =
                      EDayGroup.getDayGroup(line.time, timeZone, Locale.GERMANY);
                  ride.dayMeta_zonedHour = line.time.atZone(timeZone).getHour(); // - current hour

                  return ride;
                });

    Properties prodProps = new Properties();
    prodProps.put("bootstrap.servers", kafkaBroker);
    KafkaSerializationSchema<FilteredFlattenedTripStops> schema =
        new KafkaSerializationSchema<FilteredFlattenedTripStops>() {

          private static final long serialVersionUID = -4225343696563271056L;

          @Override
          public ProducerRecord<byte[], byte[]> serialize(
              FilteredFlattenedTripStops element, Long timestamp) {
            ObjectMapper om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            try {
              ProducerRecord<byte[], byte[]> rec =
                  new ProducerRecord<byte[], byte[]>(outputTopic, om.writeValueAsBytes(element));
              return rec;
            } catch (JsonProcessingException e) {
              e.printStackTrace();
            }
            return null;
          }
        };

    FlinkKafkaProducer<FilteredFlattenedTripStops> kafkaProducer =
        new FlinkKafkaProducer<FilteredFlattenedTripStops>(
            outputTopic, schema, prodProps, Semantic.NONE);
    rideDetails.addSink(kafkaProducer);

    env.execute();
  }
}
