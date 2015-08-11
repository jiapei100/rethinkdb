package com.rethinkdb;

import com.rethinkdb.ast.Query;
import com.rethinkdb.ast.ReqlAst;
import com.rethinkdb.proto.ErrorType;
import com.rethinkdb.proto.ResponseType;
import com.rethinkdb.response.Backtrace;

import java.util.Optional;
import java.util.function.Supplier;

public class ErrorBuilder {
    final String msg;
    final ResponseType responseType;
    Optional<Backtrace> backtrace = Optional.empty();
    Optional<ErrorType> errorType = Optional.empty();
    Optional<ReqlAst> term = Optional.empty();

    public ErrorBuilder(String msg, ResponseType responseType) {
        this.msg = msg;
        this.responseType = responseType;
    }

    public ErrorBuilder setBacktrace(Optional<Backtrace> backtrace) {
        this.backtrace = backtrace;
        return this;
    }

    public ErrorBuilder setErrorType(Optional<ErrorType> errorType) {
        this.errorType = errorType;
        return this;
    }

    public ErrorBuilder setTerm(Query query) {
        this.term = query.term;
        return this;
    }

    public ReqlError build() {
        assert (msg != null);
        assert (responseType != null);
        Supplier<ReqlError> con;
        switch (responseType) {
            case CLIENT_ERROR:
                con = ReqlClientError::new;
                break;
            case COMPILE_ERROR:
                con = ReqlCompileError::new;
                break;
            case RUNTIME_ERROR: {
                con = errorType.<Supplier<ReqlError>>map(et -> {
                    switch (et) {
                        case INTERNAL:
                            return ReqlInternalError::new;
                        case RESOURCE:
                            return ReqlResourceLimitError::new;
                        case LOGIC:
                            return ReqlQueryLogicError::new;
                        case NON_EXISTENCE:
                            return ReqlNonExistenceError::new;
                        case OP_FAILED:
                            return ReqlOpFailedError::new;
                        case OP_INDETERMINATE:
                            return ReqlOpIndeterminateError::new;
                        case USER:
                            return ReqlUserError::new;
                        default:
                            return ReqlRuntimeError::new;
                    }
                }).orElse(ReqlRuntimeError::new);
            }
            default:
                con = ReqlError::new;
        }
        ReqlError res = con.get();
        backtrace.ifPresent(res::setBacktrace);
        term.ifPresent(res::setTerm);
        return res;
    }
}