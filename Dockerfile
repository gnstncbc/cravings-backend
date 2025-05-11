# 1. Aşama: Maven imajı ile uygulamayı derleyin
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# ARG olarak build esnasında değer alacak
ARG JWT_SECRET

# ENV olarak runtime ortamına aktarılacak
ENV JWT_SECRET=${JWT_SECRET}
# pom.xml ve kaynak dosyalarını kopyalayın
COPY pom.xml .
COPY src ./src

# Testleri atlayarak uygulamayı paketleyin
RUN mvn clean package -DskipTests

# 2. Aşama: Derlenmiş jar dosyasını çalıştırın
FROM openjdk:17-jdk-alpine
WORKDIR /app

# Oluşan jar dosyasını build aşamasından kopyalayın.
# NOT: "cravings-0.0.1-SNAPSHOT.jar" dosya adını pom.xml dosyanızdaki artifact adıyla uyumlu olacak şekilde güncelleyin.
COPY --from=build /app/target/cravings-0.0.1-SNAPSHOT.jar app.jar

# Uygulamanın dinleyeceği portu açın
EXPOSE 8080

# Uygulamayı çalıştırın
ENTRYPOINT ["java", "-jar", "app.jar"]