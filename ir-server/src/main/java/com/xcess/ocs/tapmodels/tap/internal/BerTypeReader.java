package com.xcess.ocs.tapmodels.tap.internal;

import com.beanit.asn1bean.ber.BerLength;
import com.beanit.asn1bean.ber.BerTag;
import com.beanit.asn1bean.ber.types.BerType;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

public final class BerTypeReader {

    private BerTypeReader() {
    }

    private static InputStream openPossiblyCompressed(Path path, OpenOption... options) throws IOException {
        InputStream fileIn = new BufferedInputStream(Files.newInputStream(path, options));
        fileIn.mark(2);
        int b1 = fileIn.read();
        int b2 = fileIn.read();
        fileIn.reset();

        return (b1 == 0x1f && b2 == 0x8b)
                ? new GZIPInputStream(fileIn)
                : fileIn;
    }

    public static <T extends BerType> Optional<T> readByTag(
            Path path,
            BerTag tag,
            Supplier<T> factory,
            OpenOption... options
    ) throws IOException {
        // TLV-aware selective decoder (as discussed earlier)
        try (InputStream in = new BufferedInputStream(openPossiblyCompressed(path, options))) {
            if (tag == null) {
                // full decode
                T instance = factory.get();
                instance.decode(in);
                return Optional.of(instance);
            }

            while (in.available() > 0) {
                in.mark(32); // Ensure enough bytes TVL (tag [1-4] + length [1-5] + Value [?])

                BerTag berTag = new BerTag();
                int tagBytes = berTag.decode(in);

                BerLength berLength = new BerLength();
                int lengthBytes = berLength.decode(in);

                if (berTag.equals(tag)) {
                    in.reset(); // rewind to start of tag
                    T instance = factory.get();
                    instance.decode(in); // decode entire structure
                    return Optional.of(instance);
                } else {
                    // skip payload if tag doesn't match
                    long toSkip = berLength.val;
                    long skipped = in.skip(toSkip);
                    if (skipped < toSkip) {
                        throw new EOFException("Unexpected end of stream while skipping payload");
                    }
                }
            }
            return Optional.empty();
        }
    }

    public static List<BerTag> scanTags(Path path, OpenOption... options) throws IOException {
        List<BerTag> tags = new ArrayList<>();

        try (InputStream in = new BufferedInputStream(openPossiblyCompressed(path, options))) {
            while (in.available() > 0) {
                // Decode tag
                BerTag berTag = new BerTag();
                int tagBytes = berTag.decode(in);
                tags.add(berTag);

                // Decode length
                BerLength berLength = new BerLength();
                int lengthBytes = berLength.decode(in);

                // Skip over the value payload
                long toSkip = berLength.val;
                long skipped = in.skip(toSkip);
                if (skipped < toSkip) {
                    throw new EOFException("Unexpected end of stream while skipping payload");
                }
            }
        }
        return tags;

    }
}