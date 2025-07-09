package com.azyrod.rpa_whitelist.config.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import discord4j.common.util.Snowflake;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class SnowflakeDeserializer extends StdDeserializer<Snowflake> {

    public SnowflakeDeserializer() {
        this(null);
    }

    public SnowflakeDeserializer(Class<Snowflake> t) {
        super(t);
    }

    @Override
    public Snowflake deserialize(@NotNull JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        long id = node.get("id").numberValue().longValue();

        return Snowflake.of(id);
    }
}
