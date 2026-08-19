package com.xcess.ocs.tapmodels.tap;

import com.xcess.ocs.tapmodels.rap.RapDataInterChange;
import com.xcess.ocs.tapmodels.tap.internal.BerTypeReader;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Optional;

public class RapFiles {

    private RapFiles() {
    }

    public static Optional<RapDataInterChange> read(Path path, OpenOption... options) throws IOException {
        return BerTypeReader.readByTag(path, null, RapDataInterChange::new, options);
    }
}
