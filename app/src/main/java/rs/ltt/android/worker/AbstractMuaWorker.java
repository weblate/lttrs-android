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
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLException;

import rs.ltt.android.cache.DatabaseCache;
import rs.ltt.android.database.AppDatabase;
import rs.ltt.android.database.LttrsDatabase;
import rs.ltt.android.entity.AccountWithCredentials;
import rs.ltt.jmap.client.api.MethodErrorResponseException;
import rs.ltt.jmap.client.session.FileSessionCache;
import rs.ltt.jmap.common.method.MethodErrorResponse;
import rs.ltt.jmap.common.method.error.StateMismatchMethodErrorResponse;
import rs.ltt.jmap.mua.Mua;

public abstract class AbstractMuaWorker extends Worker {

    public static final String TAG_EMAIL_MODIFICATION = "email_modification";

    static final String ACCOUNT_KEY = "account";

    private final Long account;

    AbstractMuaWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        final Data data = getInputData();
        if (data.hasKeyWithValueOfType(ACCOUNT_KEY, Long.class)) {
            this.account = data.getLong(ACCOUNT_KEY, 0L);
        } else {
            throw new IllegalStateException("Missing required account");
        }
    }

    static boolean shouldRetry(final ExecutionException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof MethodErrorResponseException) {
            final MethodErrorResponse methodError = ((MethodErrorResponseException) cause).getMethodErrorResponse();
            return methodError instanceof StateMismatchMethodErrorResponse;
        }
        return isNetworkIssue(cause);
    }

    private static boolean isNetworkIssue(final Throwable cause) {
        return cause instanceof SocketTimeoutException
                || cause instanceof SocketException
                || cause instanceof SSLException;
    }

    protected LttrsDatabase getDatabase() {
        return LttrsDatabase.getInstance(getApplicationContext(), this.account);
    }

    protected Mua getMua() {
        final AccountWithCredentials account = AppDatabase.getInstance(getApplicationContext()).accountDao().getAccount(this.account);
        return Mua.builder()
                .username(account.username)
                .password(account.password)
                .accountId(account.accountId)
                .sessionResource(account.sessionResource)
                .cache(new DatabaseCache(getDatabase()))
                .sessionCache(new FileSessionCache(getApplicationContext().getCacheDir()))
                .build();
    }
}
