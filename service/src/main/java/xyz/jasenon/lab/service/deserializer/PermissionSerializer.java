package xyz.jasenon.lab.service.deserializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.jasenon.lab.service.constants.Permissions;

import java.io.IOException;

public class PermissionSerializer extends JsonSerializer<Permissions> {

    @Override
    public void serialize(Permissions arg0, JsonGenerator arg1, SerializerProvider arg2) throws IOException {

        arg1.writeStartObject();
        arg1.writeObjectField("description",arg0.getDescription());
        arg1.writeObjectField("id",arg0.getId());
        arg1.writeObjectField("parentId",arg0.getParentId());
        arg1.writeEndObject();

    }


}