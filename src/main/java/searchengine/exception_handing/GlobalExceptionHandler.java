package searchengine.exception_handing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.exception_handing.indexing.*;
import searchengine.exception_handing.search.*;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<EmptySearchRequest> handleEmptySearchRequest(EmptySearchRequestException exception) {
        return new ResponseEntity<>(new EmptySearchRequest(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<NoSitesInConfig> handleNoSitesInConfig(NoSitesInConfigException exception) {
        return new ResponseEntity<>(new NoSitesInConfig(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<SiteOutsideConfig> handleSiteOutsideConfig(SiteOutsideConfigException exception) {
        return new ResponseEntity<>(new SiteOutsideConfig(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<SiteNotIndexed> handleSiteNotIndexed(SiteNotIndexedException exception) {
        return new ResponseEntity<>(new SiteNotIndexed(exception.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<IndexingStatus> handleIndexingStatus(IndexingStatusException exception) {
        return new ResponseEntity<>(new IndexingStatus(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<PageOutsideConfig> handlePageOutsideConfig(PageOutsideConfigException exception) {
        return new ResponseEntity<>(new PageOutsideConfig(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<PageCantBeIndexed> handlePageCantBeIndexed(PageCantBeIndexedException exception) {
        return new ResponseEntity<>(new PageCantBeIndexed(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<IndexingInProgress> handleIndexingInProgress(IndexingInProgressException exception) {
        return new ResponseEntity<>(new IndexingInProgress(), HttpStatus.BAD_REQUEST);
    }
}
