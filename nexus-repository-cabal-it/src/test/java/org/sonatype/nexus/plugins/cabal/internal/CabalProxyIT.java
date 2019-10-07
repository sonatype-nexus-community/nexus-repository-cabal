/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2019-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.cabal.internal;

import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.content;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.nexus.plugins.cabal.internal.util.CabalPathUtils.PACKAGE_FILENAME;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class CabalProxyIT
    extends CabalITSupport
{
  // @todo Change test path for your format
  private static final String TEST_PATH = "some/valid/path/for/your/remote/" + PACKAGE_FILENAME;

  private static final String PACKAGE_ALGO_RHYTHM_0_1_0_0_ALGO_RHYTHM_0_1_0_0_TAR_GZ = "package/AlgoRhythm-0.1.0.0/AlgoRhythm-0.1.0.0.tar.gz";

  private static final String PACKAGE_ALGO_RHYTHM_CABAL = "package/AlgoRhythm-0.1.0.0/AlgoRhythm.cabal";

  private static final String INCREMENTAL_INDEX = "01-index.tar.gz";

  private static final String TIMESTAMP = "timestamp.json";

  private static final String SNAPSHOT = "snapshot.json";

  private static final String MIRRORS = "mirrors.json";

  private static final String ROOT = "root.json";

  private CabalClient proxyClient;

  private Repository proxyRepo;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-cabal")
    );
  }

  @Test
  public void unresponsiveRemoteProduces404() throws Exception {
    Server server = Server.withPort(0).serve("/*")
        .withBehaviours(error(HttpStatus.NOT_FOUND))
        .start();
    try {
      proxyRepo = repos.createCabalProxy("cabal-test-proxy-notfound", server.getUrl().toExternalForm());
      proxyClient = cabalClient(proxyRepo);
      MatcherAssert.assertThat(FormatClientSupport.status(proxyClient.get(TEST_PATH)), is(HttpStatus.NOT_FOUND));
    }
    finally {
      server.stop();
    }
  }


  @Test
  public void retrievePackageWhenRemoteOnline() throws Exception {
    Server server = Server.withPort(0)
        .serve("/" + PACKAGE_ALGO_RHYTHM_0_1_0_0_ALGO_RHYTHM_0_1_0_0_TAR_GZ)
        .withBehaviours(Behaviours.file(testData.resolveFile("AlgoRhythm-0.1.0.0.tar.gz")))
        .start();
    try {
      proxyRepo = repos.createCabalProxy("cabal-test-proxy-online", server.getUrl().toExternalForm());
      proxyClient = cabalClient(proxyRepo);
      proxyClient.get(PACKAGE_ALGO_RHYTHM_0_1_0_0_ALGO_RHYTHM_0_1_0_0_TAR_GZ);
    }
    finally {
      server.stop();
    }
    final Asset asset = findAsset(proxyRepo, PACKAGE_ALGO_RHYTHM_0_1_0_0_ALGO_RHYTHM_0_1_0_0_TAR_GZ);
    assertThat(asset.format(), is("cabal"));
    assertThat(asset.name(), is("package/AlgoRhythm-0.1.0.0/AlgoRhythm-0.1.0.0.tar.gz"));
    assertThat(asset.contentType(), is("application/x-gzip"));
    assertThat(status(proxyClient.get(PACKAGE_ALGO_RHYTHM_0_1_0_0_ALGO_RHYTHM_0_1_0_0_TAR_GZ)), is(200));
  }

  @Test
  public void retrieveCabalFileWhenRemoteOnline() throws Exception {
    Server server = Server.withPort(0)
        .serve("/" + PACKAGE_ALGO_RHYTHM_CABAL)
        .withBehaviours(Behaviours.file(testData.resolveFile("AlgoRhythm.cabal")))
        .start();
    try {
      proxyRepo = repos.createCabalProxy("cabal-test-proxy-online", server.getUrl().toExternalForm());
      proxyClient = cabalClient(proxyRepo);
      proxyClient.get(PACKAGE_ALGO_RHYTHM_CABAL);
    }
    finally {
      server.stop();
    }
    final Asset asset = findAsset(proxyRepo, PACKAGE_ALGO_RHYTHM_CABAL);
    assertThat(status(proxyClient.get(PACKAGE_ALGO_RHYTHM_CABAL)), is(200));
  }

  @Test
  public void retrieveIncrementalIndexWhenRemoteOnline() throws Exception {
    Server server = Server.withPort(0)
        .serve("/" + INCREMENTAL_INDEX)
        .withBehaviours(Behaviours.file(testData.resolveFile("01-index.tar.gz")))
        .start();
    try {
      proxyRepo = repos.createCabalProxy("cabal-test-proxy-online", server.getUrl().toExternalForm());
      proxyClient = cabalClient(proxyRepo);
      proxyClient.get(INCREMENTAL_INDEX);
    }
    finally {
      server.stop();
    }
    final Asset asset = findAsset(proxyRepo, INCREMENTAL_INDEX);
    assertThat(status(proxyClient.get(INCREMENTAL_INDEX)), is(200));
  }

  @Test
  public void retrievetimeStampWhenRemoteOnline() throws Exception {
    Server server = Server.withPort(0)
        .serve("/" + TIMESTAMP)
        .withBehaviours(Behaviours.file(testData.resolveFile("timestamp.json")))
        .start();
    try {
      proxyRepo = repos.createCabalProxy("cabal-test-proxy-online", server.getUrl().toExternalForm());
      proxyClient = cabalClient(proxyRepo);
      proxyClient.get(TIMESTAMP);
    }
    finally {
      server.stop();
    }
    final Asset asset = findAsset(proxyRepo, TIMESTAMP);
    assertThat(status(proxyClient.get(TIMESTAMP)), is(200));
  }

  @Test
  public void retrieveMirrorsJSONWhenRemoteOnline() throws Exception {
    Server server = Server.withPort(0)
        .serve("/" + MIRRORS)
        .withBehaviours(Behaviours.file(testData.resolveFile("mirrors.json")))
        .start();
    try {
      proxyRepo = repos.createCabalProxy("cabal-test-proxy-online", server.getUrl().toExternalForm());
      proxyClient = cabalClient(proxyRepo);
      proxyClient.get(MIRRORS);
    }
    finally {
      server.stop();
    }
    final Asset asset = findAsset(proxyRepo, MIRRORS);
    assertThat(status(proxyClient.get(MIRRORS)), is(200));
  }

  @Test
  public void retrieveRootJSONWhenRemoteOnline() throws Exception {
    Server server = Server.withPort(0)
        .serve("/" + ROOT)
        .withBehaviours(Behaviours.file(testData.resolveFile("root.json")))
        .start();
    try {
      proxyRepo = repos.createCabalProxy("cabal-test-proxy-online", server.getUrl().toExternalForm());
      proxyClient = cabalClient(proxyRepo);
      proxyClient.get(ROOT);
    }
    finally {
      server.stop();
    }
    final Asset asset = findAsset(proxyRepo, ROOT);
    assertThat(status(proxyClient.get(ROOT)), is(200));
  }

  //@Test
  //public void retrieveCabalWhenRemoteOffline() throws Exception {
  //  Server server = Server.withPort(0).serve("/*")
  //      .withBehaviours(content("Response"))
  //      .start();
  //  try {
  //    proxyRepo = repos.createCabalProxy("cabal-test-proxy-offline", server.getUrl().toExternalForm());
  //    proxyClient = cabalClient(proxyRepo);
  //    proxyClient.get(TEST_PATH);
  //  }
  //  finally {
  //    server.stop();
  //  }
  //  assertThat(status(proxyClient.get(TEST_PATH)), is(200));
  //}
}
