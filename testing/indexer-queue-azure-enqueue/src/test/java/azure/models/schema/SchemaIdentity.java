package azure.models.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.StringJoiner;

@Getter
@Setter
public class SchemaIdentity {

    private String authority;
    private String source;
    private String entityType;
    private String schemaVersionMajor;
    private String schemaVersionMinor;
    private String schemaVersionPatch;


    @JsonIgnore
    public String getId() {
        return authority + ":" + source + ":" + entityType + ":" +
            schemaVersionMajor + "." + schemaVersionMinor + "." + schemaVersionPatch;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SchemaIdentity.class.getSimpleName() + "[", "]")
            .add("AUTHORITY='" + authority + "'")
            .add("SOURCE='" + source + "'")
            .add("ENTITY_TYPE='" + entityType + "'")
            .add("schemaVersionMajor='" + schemaVersionMajor + "'")
            .add("schemaVersionMinor='" + schemaVersionMinor + "'")
            .add("schemaVersionPatch='" + schemaVersionPatch + "'")
            .toString();
    }
}
