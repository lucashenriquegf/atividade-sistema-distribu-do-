FROM eclipse-temurin:17-jdk-jammy AS compilacao
WORKDIR /app
COPY src/ src/
RUN javac --release 17 -encoding UTF-8 -d out src/*.java

# A imagem final precisa apenas do Java para executar as classes já compiladas.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=compilacao /app/out/ out/
USER 10001:10001
EXPOSE 5000
ENTRYPOINT ["java", "-cp", "out"]
CMD ["Servidor"]
