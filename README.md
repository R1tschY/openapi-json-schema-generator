# openapi-json-schema-generator

*Generate JSON schema from an OpenAPI schema*

Only supports OpenAPI v3.0 Schema Objects on Component Object.

## Features

### Generate for different JSON Schema Drafts

  * JSON Schema Draft 4
  * JSON Schema Draft 6
    * Changed semantic of `exclusiveMinimum` and `exclusiveMaximum`
  * JSON Schema Draft 7
    * Support for `readOnly` and `writeOnly`
    * Support for `contentEncoding` (Used for `format: byte`)
  * JSON Schema Draft 2019-09
    * Use `#/$defs` instead of `#/definitions` for definitions
    * Support for `deprecated`
    
### Support for OpenAPIs `nullable` extension

```yaml
# ...
components:
  schemas:
    NullableString:
      type: string
      nullable: true
    NullableEnum:
      type: string
      enum: ["a", "b", "c"]
      nullable: true
```
Results in:
```json
{
  "$schema" : "https://json-schema.org/draft/2019-09/schema",
  "$defs" : {
    "NullableString" : {
      "type" : [ "string", "null" ]
    },
    "NullableEnum": {
      "type" : [ "string", "null" ],
      "enum": [ "a", "b", "c", null ]
    }
  }
}
```

### Support for OpenAPIs `example` extension

```yaml
# ...
components:
  schemas:
    String:
      type: string
      example: "abc"
```
Results in:
```json
{
  "$schema" : "https://json-schema.org/draft/2019-09/schema",
  "$defs" : {
    "StringRef" : {
      "type" : [ "string" ],
      "examples": [ "abc" ]
    }
  }
}
```

### References

References to other schemas are redirected:

```yaml
# ...
components:
  schemas:
    StringRef:
      $ref: '#/components/schemas/String'
    String:
      type: string
```
Results in:
```json
{
  "$schema" : "https://json-schema.org/draft/2019-09/schema",
  "$defs" : {
    "StringRef" : {
      "$ref" : "#/$defs/String"
    },
    "String" : {
      "type" : [ "string" ]
    }
  }
}
```
  
### Main Schema

A main schema can be selected: `openapi-json-schema-generator INPUT MySchema`

So the JSON schema can be directly used to validate a single schema.

```yaml
# ...
components:
  schemas:
    MySchema:
      type: string
```
Results in:
```json
{
  "$schema" : "https://json-schema.org/draft/2019-09/schema",
  "$ref" : "#/$defs/MySchema",
  "$defs" : {
    "MySchema" : {
      "type" : [ "string" ]
    }
  }
}
```

## Usage
```
Usage: openapi-json-schema-generator [-hV] [--exclude-read-only]
                                     [--exclude-write-only]
                                     [--json-schema-version=<jsonSchemaDraft>]
                                     INPUT [MAIN_SCHEMA]
Generate JSON schema from Open API specification
      INPUT                  Reference to OpenAPI specification in JSON or YAML
                               format
      [MAIN_SCHEMA]          Name of schema ('MySchema') or reference to schema
                               ('#/components/schemas/MySchema') to use as
                               top-level schema. Allows to use output to
                               directly validate this schema.
      --exclude-read-only    Exclude read only properties.
      --exclude-write-only   Exclude write only properties.
  -h, --help                 Show this help message and exit.
      --json-schema-version=<jsonSchemaDraft>
                             Use this JSON Schema Draft for output. Choices: 4,
                               6, 7, 2019-09
                               Default: 2019-09
  -V, --version              Print version information and exit.
```

## License

This code is under the Apache Licence v2.