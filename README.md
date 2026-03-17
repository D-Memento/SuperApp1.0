# SuperApp

SuperApp - графический файловый менеджер для Linux на Java.

## Maven-сборка

В проект добавлена сборка через Maven без изменения расположения исходных файлов.

- `pom.xml` находится в корне проекта
- исходники остаются в `System/src/main/java`
- итоговый исполняемый jar собирается в `target/SuperApp.jar`

В `pom.xml` Maven настроен на нестандартный путь к исходникам через `sourceDirectory`, поэтому переносить файлы не нужно.

## Требования

- Java 21
- Maven 3.9+

## Сборка

```bash
mvn clean package
```

После сборки jar будет лежать здесь:

```bash
target/SuperApp.jar
```

## Запуск

```bash
java -jar target/SuperApp.jar
```

или

```bash
./run-superapp.sh
```
