package ch.cyberduck.core.onedrive;

/*
 * Copyright (c) 2002-2017 iterate GmbH. All rights reserved.
 * https://cyberduck.io/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.onedrive.features.OneDriveFileIdProvider;

public class OneDriveListService implements ListService {

    private final OneDriveSession session;
    private final OneDriveFileIdProvider fileIdProvider;

    public OneDriveListService(final OneDriveSession session, final OneDriveFileIdProvider fileIdProvider) {
        this.session = session;
        this.fileIdProvider = fileIdProvider;
    }

    @Override
    public AttributedList<Path> list(final Path directory, final ListProgressListener listener) throws BackgroundException {
        if(directory.isRoot()) {
            return new OneDriveContainerListService(session).list(directory, listener);
        }
        else {
            return new OneDriveItemListService(session).list(directory, listener);
        }
    }

    @Override
    public ListService withCache(final Cache<Path> cache) {
        fileIdProvider.withCache(cache);
        return this;
    }
}
