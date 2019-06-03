package org.reveno.atp.acceptance.views;

import org.reveno.atp.api.query.QueryManager;

import java.util.Optional;
import java.util.stream.Stream;

public class ViewBase {

    protected QueryManager query;

    static <T> Stream<T> sops(Optional<T> opt) {
        return opt.map(Stream::of).orElseGet(Stream::empty);
    }

}
