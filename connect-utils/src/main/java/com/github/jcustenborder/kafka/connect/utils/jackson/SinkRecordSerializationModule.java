/**
 * Copyright © 2016 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.connect.utils.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SinkRecordSerializationModule extends SimpleModule {

  public SinkRecordSerializationModule() {
    super();
    addSerializer(SinkRecord.class, new Serializer());
    addDeserializer(SinkRecord.class, new Deserializer());
  }

  public static class Storage {
    public String topic;
    public Integer kafkaPartition;
    public Schema keySchema;
    public Object key;
    public Schema valueSchema;
    public Object value;
    public Long timestamp;
    public TimestampType timestampType = TimestampType.NO_TIMESTAMP_TYPE;
    public long offset = 1234L;
    public List<Header> headers;

    public Object value() {
      if (this.valueSchema != null) {
        return ValueHelper.value(this.valueSchema, this.value);
      } else {
        return this.value;
      }
    }

    public Object key() {
      if (this.keySchema != null) {
        return ValueHelper.value(this.keySchema, this.key);
      } else {
        return this.key;
      }
    }

    public SinkRecord build() {
      return new SinkRecord(
          topic,
          kafkaPartition,
          keySchema,
          key(),
          valueSchema,
          value(),
          this.offset,
          timestamp,
          timestampType,
          headers
      );
    }
  }

  static class Serializer extends JsonSerializer<SinkRecord> {

    @Override
    public void serialize(SinkRecord record, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
      Storage storage = new Storage();
      storage.topic = record.topic();
      storage.kafkaPartition = record.kafkaPartition();
      storage.keySchema = record.keySchema();
      storage.key = record.key();
      storage.valueSchema = record.valueSchema();
      storage.value = record.value();
      storage.timestamp = record.timestamp();
      storage.timestampType = record.timestampType();
      storage.offset = record.kafkaOffset();
      if (null != record.headers()) {
        List<Header> headers = new ArrayList<>();
        for (Header header : record.headers()) {
          headers.add(header);
        }
        storage.headers = headers;
      }
      jsonGenerator.writeObject(storage);
    }
  }

  static class Deserializer extends JsonDeserializer<SinkRecord> {
    @Override
    public SinkRecord deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
      Storage storage = jsonParser.readValueAs(Storage.class);
      return storage.build();
    }
  }
}
