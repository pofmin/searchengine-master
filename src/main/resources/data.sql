select if (
    exists(
        select distinct index_name from information_schema.statistics
        where table_schema = 'search_engine'
        and table_name = 'pages' and index_name like 'ix_page_path'
    )
    ,'select ''index ix_page_path exists'' _______;'
    ,'create index ix_page_path on pages(path(255))') into @output;
PREPARE statement1 FROM @output;
EXECUTE statement1;
DEALLOCATE PREPARE statement1;