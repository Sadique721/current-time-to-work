package com.xcess.ocs.tapmodels.tap;

import com.xcess.ocs.tapmodels.tap.internal.BerTypeReader;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Optional;

public class TapFiles {
    private TapFiles() {
    }

    public static Optional<DataInterChange> read(Path path, OpenOption... options) throws IOException {
        return BerTypeReader.readByTag(path, null, DataInterChange::new, options);
    }

    public static Optional<BatchControlInfo> batchInfo(Path path, OpenOption... options) throws IOException {
        return BerTypeReader.readByTag(path, BatchControlInfo.tag, BatchControlInfo::new, options);
    }

    public static Optional<AccountingInfo> accountingInfo(Path path, OpenOption... options) throws IOException {
        return BerTypeReader.readByTag(path, AccountingInfo.tag, AccountingInfo::new, options);
    }

    public static Optional<NetworkInfo> networkInfo(Path path, OpenOption... options) throws IOException {
        return BerTypeReader.readByTag(path, NetworkInfo.tag, NetworkInfo::new, options);
    }

    public static Optional<AuditControlInfo> auditInfo(Path path, OpenOption... options) throws IOException {
        return BerTypeReader.readByTag(path, AuditControlInfo.tag, AuditControlInfo::new, options);
    }

}