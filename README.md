# Проект searchengine: локальный поисковый движок
## Описание
Приложение searchengine позволяет проиндексировать все страницы указанных в конфигурационном файле сайтов, а затем выполнять поиск по этим сайтам.
<br/><br/>

## Стек используемых технологий
### Backend-составляющая:  
- Java  
- Maven  
- Spring Boot  
- MySQL    
### Frontend-составляющая:  
- HTML  
- CSS  
- JAVASCRIPT  
- JQUERY
<br/><br/>

## Инструкция по запуску и использованию приложения
### Запаковка в JAR-файл
Для запуска приложения локально его необходимо запаковать в JAR-файл. В среде разработки IntelliJ IDEA, например, это можно сделать во вкладке **'Maven -> Lifecycle -> Package'**. После окончания сборки JAR-файл появится в папке **'target'** проекта.
<br/><br/>

### База данных
Для функционирования приложения необходима база данных MySQL, в которой нужно создать пустую схему **'search_engine'**.
<br/><br/>

### Заполнение конфигурационного файла
Приложению необходим корректно заполненный конфигурационный файл **'application.yaml'** (находится в корне проекта). В данный файл необходимо внести следующие данные:
- Имя пользователя и пароль для подключения к базе данных (параметры ***spring.datasource.username*** и ***spring.datasource.password***);
- Список сайтов, страницы которых будут индексироваться (параметры ***indexing-settings.sites.url*** и ***indexing-settings.sites.name*** для каждого сайта; параметр ***name*** можно задавать произвольно).

Также дополнительно можно скорректировать следующие параметры:
- ***indexing-settings.frequency-threshold*** – процент от общего количества страниц сайта, превышение которого недопустимо для попадания искомого слова в итоговый поисковый запрос (например, если искомое слово встречается более чем на половине страниц сайта, а параметр задан со значением 0.5, то данное слово будет исключено из поискового запроса);
- ***snippet-length-max*** – количество слов, из которых будут состоять возвращаемые приложением сниппеты, то есть отрезки текста с найденными в них результатами поискового запроса;
- ***connection-humanizer.user-agent*** и ***connection-humanizer.referrer*** – параметры, используемые для подключения к страницам при обходе сайтов во избежание блокировок;
- ***connection-humanizer.sleep-length.minimum*** и ***connection-humanizer.sleep-length.maximum*** – нижняя и верхняя границы паузы, которую приложение делает при обходе страниц для предотвращения блокировок (приложение выбирает значение каждой паузы случайным образом в заданных рамках).
<br/><br/>
 
### Запуск приложения
Когда все вышеперечисленные пункты выполнены, приложение готово к запуску. Запустить его локально можно следующей командой:  
`java -jar [имя JAR-файла] --spring.config.location=[путь к конфигурационному файлу application.yaml]`
<br/><br/>

### Использование GUI приложения 
После запуска приложения его GUI будет доступен по ссылке [localhost:8080](http://localhost:8080/). При переходе по данной ссылке автоматически открывается вкладка **'DASHBOARD'**, на которой представлена текущая статистика приложения. При первом запуске до проведения индексации статистика будет нулевая:

![1 - Нулевая статистика](https://user-images.githubusercontent.com/107081331/235676449-7d595f39-354c-487b-b4e8-aac832c846b0.png)

Для запуска индексации по сайтам, указанным в конфигурационном файле, необходимо перейти во вкладку **'MANAGEMENT'** и нажать кнопку **'START INDEXING'**:

![2 - Запуск индексации](https://user-images.githubusercontent.com/107081331/235676868-d404f3e8-d10c-42b0-af27-9ce12fbc398d.png)

После запуска индексации кнопка **'START INDEXING'** меняется на **'STOP INDEXING'**:

![3 - Смена кнопки запуска индексации](https://user-images.githubusercontent.com/107081331/235676966-cddf5e33-3eab-4b1f-8295-9d02af2612f2.png)

С помощью этой кнопки индексацию можно досрочно остановить. В этом случае индексация прерывается с соответствующей ошибкой, и статистика по индексируемым сайтам будет выглядеть следующим образом:

![4 - Статистика при прерывании](https://user-images.githubusercontent.com/107081331/235677102-671abaa0-d166-4f2c-9fa6-f03f3de192f7.png)

Если сайт находится в процессе индексации, он будет иметь статус ***INDEXING***. Если приложение не может проиндексировать какой-либо сайт, его статус сменится на ***FAILED***, а в соответствующем поле будет отображаться текст ошибки:

![5 - Статистика в процессе](https://user-images.githubusercontent.com/107081331/235677205-a5b9c1a5-8252-420e-9313-33a57e79c986.png)

По окончании индексации вкладка **'DASHBOARD'** будет содержать общую статистику (общее количество проиндексированных сайтов, общее количество страниц на них и общее количество лемм – исходных форм всех слов, которые встречаются на этих страницах), а также детальную статистику по каждому из сайтов:

![stat_success](https://github.com/pofmin/searchengine-master/assets/107081331/d2347cf9-7860-4c4b-9520-98557fc69b5c)

До индексации или после ее завершения можно проиндексировать отдельную страницу, введя ее адрес в указанное поле и нажав кнопку **'ADD/UPDATE'**. Проиндексировать можно страницы только тех сайтов, которые указаны в конфигурационном файле:

![7 - Успешная индексация одной страницы](https://user-images.githubusercontent.com/107081331/235677410-6802dac0-a110-47d2-8afd-a35a7055ea8a.png)

Приложение не позволяет индексировать отдельные страницы в тот момент, когда проходит общая индексация:

![8 - Неуспешная индексация одной страницы](https://user-images.githubusercontent.com/107081331/235677617-74a139db-9e50-49c5-9c3a-2d4df90174ed.png)

Если введен неверный формат URL, выводится соответствующая ошибка:

![Wrong URL format](https://github.com/pofmin/searchengine-master/assets/107081331/24a01fa4-bd68-4e73-a81a-658459c83147)

Индексация не производится, если введена ссылка на сайт, которого нет в конфигурационном файле:

![Outside config](https://github.com/pofmin/searchengine-master/assets/107081331/925b76fa-f29b-4cd9-a4c8-4bb7c7b40c0e)

Поиск производится на вкладке **'SEARCH'**. Искать можно как по всем проиндексированным сайтам:

![9 - Поиск по всем сайтам](https://user-images.githubusercontent.com/107081331/235677714-8eb59806-a34e-457f-a1fc-3f97d77c9b65.png)

так и по какому-либо одному из них:

![10 - Поиск по одному сайту](https://user-images.githubusercontent.com/107081331/235677784-0fe75d87-fc4e-46b4-bf9f-dca7d48dbdbe.png)

Результаты выводятся в виде списка по десять результатов. Для просмотра следующей порции результатов необходимо нажать соответствующую кнопку внизу списка:

![11 - Вывод следующей порции](https://user-images.githubusercontent.com/107081331/235677888-39412901-dd1e-4ff5-817a-25c8a3de4bdb.png)
