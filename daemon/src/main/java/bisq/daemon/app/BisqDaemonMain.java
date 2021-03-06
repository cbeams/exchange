/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.daemon.app;

import bisq.core.app.BisqExecutable;
import bisq.core.app.CoreModule;
import bisq.core.grpc.BisqGrpcServer;
import bisq.core.grpc.CoreApi;

import bisq.common.UserThread;
import bisq.common.app.AppModule;
import bisq.common.app.Version;
import bisq.common.setup.CommonSetup;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqDaemonMain extends BisqExecutable {

    protected BisqDaemon bisqDaemon;

    public BisqDaemonMain() {
        super("Bisq Daemon", "bisqd", "Bisq", Version.VERSION);
    }

    public static void main(String[] args) throws Exception {
        new BisqDaemonMain().execute(args);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // First synchronous execution tasks
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void configUserThread() {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(this.getClass().getSimpleName())
                .setDaemon(false)
                .build();
        UserThread.setExecutor(Executors.newSingleThreadExecutor(threadFactory));
    }

    @Override
    protected void launchApplication() {
        bisqDaemon = new BisqDaemon();
        CommonSetup.setup(BisqDaemonMain.this.bisqDaemon);

        UserThread.execute(this::onApplicationLaunched);
    }

    @Override
    protected void onApplicationLaunched() {
        super.onApplicationLaunched();
        bisqDaemon.setGracefulShutDownHandler(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // We continue with a series of synchronous execution tasks
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected AppModule getModule() {
        return new CoreModule(config);
    }

    @Override
    protected void applyInjector() {
        super.applyInjector();

        bisqDaemon.setInjector(injector);
    }

    @Override
    protected void startApplication() {
        // We need to be in user thread! We mapped at launchApplication already...
        bisqDaemon.startApplication();

        // In headless mode we don't have an async behaviour so we trigger the setup by calling onApplicationStarted
        onApplicationStarted();
    }

    @Override
    protected void onApplicationStarted() {
        super.onApplicationStarted();

        CoreApi coreApi = injector.getInstance(CoreApi.class);
        new BisqGrpcServer(coreApi).start();
    }

    @Override
    public void onSetupComplete() {
        log.info("onSetupComplete");
    }
}
