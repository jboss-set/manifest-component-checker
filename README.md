# Channel component verifier
Project used to verify all artifacts versions in a Wildfly Channel against PNC components

# Building

`mvn clean install`

# Usage
```
java -jar target/component-mapper-1.0.0-SNAPSHOT-shaded.jar \
  --pnc-url <ULR_OF_PNC_ENDPOINT> \
  --manifest-url <URL_OF_TESTED_MANIFEST>
```

