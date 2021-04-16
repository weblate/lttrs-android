/*
 * Copyright 2019 Daniel Gultsch
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rs.ltt.android.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

import rs.ltt.android.entity.EmailWithMailboxes;
import rs.ltt.jmap.common.entity.IdentifiableMailboxWithRole;

public class CopyToMailboxWorker extends AbstractMailboxModificationWorker {

    private static final String THREAD_ID_KEY = "threadId";
    private static final Logger LOGGER = LoggerFactory.getLogger(CopyToMailboxWorker.class);
    private final String mailboxId;

    public CopyToMailboxWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = getInputData();
        this.mailboxId = data.getString(MAILBOX_ID_KEY);
    }

    public static Data data(Long account, String threadId, IdentifiableMailboxWithRole mailbox) {
        return new Data.Builder()
                .putLong(ACCOUNT_KEY, account)
                .putString(THREAD_ID_KEY, threadId)
                .putString(MAILBOX_ID_KEY, mailbox.getId())
                .build();
    }

    @Override
    protected ListenableFuture<Boolean> modify(List<EmailWithMailboxes> emails) throws ExecutionException {
        final IdentifiableMailboxWithRole mailbox = Preconditions.checkNotNull(
                getDatabase().mailboxDao().getMailbox(this.mailboxId),
                String.format("Unable to find cached mailbox with id %s", this.mailboxId)
        );
        LOGGER.info("Modifying {} emails in thread {}", emails.size(), threadId);
        return getMua().copyToMailbox(emails, mailbox);
    }
}
