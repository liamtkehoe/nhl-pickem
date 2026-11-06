package io.github.liamtkehoe.nhl_pickem.web;

import io.github.liamtkehoe.nhl_pickem.service.ForbiddenException;
import io.github.liamtkehoe.nhl_pickem.service.InvalidRequestException;
import io.github.liamtkehoe.nhl_pickem.service.NotFoundException;
import io.github.liamtkehoe.nhl_pickem.service.PickException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail invalid(InvalidRequestException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.problems().size() + " problem(s) with this request");
        problem.setProperty("problems", e.problems());

        return problem;
    }

    @ExceptionHandler(PickException.class)
    public ProblemDetail conflict(PickException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail forbidden(ForbiddenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }
}
