# Étape 1 : Construire l'application avec Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copier le fichier pom.xml et les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline

# Copier le code source
COPY src ./src

# Construire l'application (ignorer les tests)
RUN mvn clean package -DskipTests

# Étape 2 : Image légère pour l'exécution
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Exposer le port
EXPOSE 8083

# Démarrer l'application
CMD ["java", "-jar", "app.jar"]