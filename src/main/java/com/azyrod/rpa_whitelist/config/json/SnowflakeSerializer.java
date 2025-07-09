package com.azyrod.rpa_whitelist.config.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import discord4j.common.util.Snowflake;

import java.io.IOException;

public class SnowflakeSerializer extends StdSerializer<Snowflake> {

    public SnowflakeSerializer() {
        this(null);
    }

    public SnowflakeSerializer(Class<Snowflake> t) {
        super(t);
    }

    @Override
    public void serialize(Snowflake value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeNumberField("id", value.asLong());
        jgen.writeEndObject();
    }
}
