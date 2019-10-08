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
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
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

  private static final String VERSION = "0.1.0.0";

  private static final String NAME_PACKAGE = "AlgoRhythm";

  private static final String EXTENSION_TAR_GZ = ".tar.gz";

  private static final String EXTENSION_CABAL = ".cabal";

  private static final String EXTENSION_JSON = ".json";

  private static final String NAME_INCREMENTAL_INDEX = "01-index";

  private static final String NAME_TIMESTAMP = "timestamp";

  private static final String NAME_SNAPSHOT = "snapshot";

  private static final String NAME_MIRRORS = "mirrors";

  private static final String NAME_ROOT = "root";

  private static final String FILE_INCREMENTAL_INDEX = NAME_INCREMENTAL_INDEX + EXTENSION_TAR_GZ;

  private static final String FILE_TIMESTAMP = NAME_TIMESTAMP + EXTENSION_JSON;

  private static final String FILE_SNAPSHOT = NAME_SNAPSHOT + EXTENSION_JSON;

  private static final String FILE_MIRRORS = NAME_MIRRORS + EXTENSION_JSON;

  private static final String FILE_ROOT = NAME_ROOT + EXTENSION_JSON;

  private static final String FILE_TAR_GZ_PACKAGE = NAME_PACKAGE + "-" + VERSION + EXTENSION_TAR_GZ;

  private static final String FILE_CABAL_PACKAGE = NAME_PACKAGE + EXTENSION_CABAL;
  
  private static final String DIRECTORY_PACKAGE = "package/" + NAME_PACKAGE + "-" + VERSION + "/";
  
  private static final String DIRECTORY_INVALID = "this/is/a/bad/path/";

  private static final String PATH_TAR_GZ_PACKAGE = DIRECTORY_PACKAGE + FILE_TAR_GZ_PACKAGE;

  private static final String PATH_CABAL_PACKAGE = DIRECTORY_PACKAGE + FILE_CABAL_PACKAGE;

  private CabalClient proxyClient;

  private Repository proxyRepo;

  private Server server;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-cabal")
    );
  }

  @Before
  public void setup() throws Exception {
    server = Server.withPort(0)
        .serve("/" + PATH_TAR_GZ_PACKAGE)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_TAR_GZ_PACKAGE)))
        .serve("/" + PATH_CABAL_PACKAGE)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_CABAL_PACKAGE)))
        .serve("/" + FILE_INCREMENTAL_INDEX)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_INCREMENTAL_INDEX)))
        .serve("/" + FILE_TIMESTAMP)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_TIMESTAMP)))
        .serve("/" + FILE_SNAPSHOT)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_SNAPSHOT)))
        .serve("/" + FILE_MIRRORS)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_MIRRORS)))
        .serve("/" + FILE_ROOT)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_ROOT)))
        .start();

    proxyRepo = repos.createCabalProxy("cabal-test-proxy-online", server.getUrl().toExternalForm());
    proxyClient = cabalClient(proxyRepo);
  }

  @Test
  public void unresponsiveRemoteProduces404() throws Exception {
    Server serverUnresponsive = Server.withPort(0).serve("/*")
        .withBehaviours(error(HttpStatus.NOT_FOUND))
        .start();
    try {
      Repository proxyRepoUnresponsive =
          repos.createCabalProxy("cabal-test-proxy-notfound", serverUnresponsive.getUrl().toExternalForm());
      CabalClient proxyClientUnresponsive = cabalClient(proxyRepoUnresponsive);
      MatcherAssert.assertThat(FormatClientSupport.status(proxyClientUnresponsive.get(PATH_TAR_GZ_PACKAGE)), is(HttpStatus.NOT_FOUND));
    }
    finally {
      serverUnresponsive.stop();
    }
  }


  @Test
  public void retrievePackageWhenRemoteOnline() throws Exception {
    proxyClient.get(PATH_TAR_GZ_PACKAGE);
    assertThat(status(proxyClient.get(PATH_TAR_GZ_PACKAGE)), is(200));

    final Component component = findComponent(proxyRepo, NAME_PACKAGE);
    assertThat(component.version(), is(VERSION));
    assertThat(component.name(), is (NAME_PACKAGE));

    final Asset asset = findAsset(proxyRepo, PATH_TAR_GZ_PACKAGE);
    assertThat(asset.format(), is("cabal"));
    assertThat(asset.name(), is(PATH_TAR_GZ_PACKAGE));
    assertThat(asset.contentType(), is("application/x-gzip"));
  }

  @Test
  public void retrieveCabalFileWhenRemoteOnline() throws Exception {
    proxyClient.get(PATH_CABAL_PACKAGE);

    final Asset asset = findAsset(proxyRepo, PATH_CABAL_PACKAGE);
    assertThat(status(proxyClient.get(PATH_CABAL_PACKAGE)), is(200));
  }

  @Test
  public void retrieveIncrementalIndexWhenRemoteOnline() throws Exception {
    proxyClient.get(FILE_INCREMENTAL_INDEX);

    final Asset asset = findAsset(proxyRepo, FILE_INCREMENTAL_INDEX);
    assertThat(status(proxyClient.get(FILE_INCREMENTAL_INDEX)), is(200));
  }

  @Test
  public void retrievetimeStampWhenRemoteOnline() throws Exception {
    proxyClient.get(FILE_TIMESTAMP);

    final Asset asset = findAsset(proxyRepo, FILE_TIMESTAMP);
    assertThat(status(proxyClient.get(FILE_TIMESTAMP)), is(200));
  }

  @Test
  public void retrieveMirrorsJSONWhenRemoteOnline() throws Exception {
    proxyClient.get(FILE_MIRRORS);

    final Asset asset = findAsset(proxyRepo, FILE_MIRRORS);
    assertThat(status(proxyClient.get(FILE_MIRRORS)), is(200));
  }

  @Test
  public void retrieveRootJSONWhenRemoteOnline() throws Exception {
    proxyClient.get(FILE_ROOT);

    final Asset asset = findAsset(proxyRepo, FILE_ROOT);
    assertThat(status(proxyClient.get(FILE_ROOT)), is(200));
  }

  @Test
  public void retrieveCabalWhenRemoteOffline() throws Exception {
    try {
      proxyRepo = repos.createCabalProxy("cabal-test-proxy-offline", server.getUrl().toExternalForm());
      proxyClient.get(PATH_TAR_GZ_PACKAGE);
    }
    finally {
      server.stop();
    }
    assertThat(status(proxyClient.get(PATH_TAR_GZ_PACKAGE)), is(200));
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }
}
