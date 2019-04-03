package org.reveno.atp.api.exceptions;

import java.io.File;

public class FileNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1718559297558833658L;

    public FileNotFoundException(File file, Throwable e) {
        super(String.format("File %s not found.", file.getAbsolutePath()), e);
    }

}
