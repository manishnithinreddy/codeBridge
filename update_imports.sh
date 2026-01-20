#!/bin/bash

# Update javax.persistence to jakarta.persistence
find codebridge-session-service/src -name "*.java" -exec sed -i 's/javax\.persistence/jakarta.persistence/g' {} \;

# Update javax.validation to jakarta.validation
find codebridge-session-service/src -name "*.java" -exec sed -i 's/javax\.validation/jakarta.validation/g' {} \;

# Update any other javax packages to jakarta
find codebridge-session-service/src -name "*.java" -exec sed -i 's/javax\.servlet/jakarta.servlet/g' {} \;
find codebridge-session-service/src -name "*.java" -exec sed -i 's/javax\.annotation/jakarta.annotation/g' {} \;

