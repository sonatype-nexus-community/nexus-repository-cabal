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
package org.sonatype.nexus.plugins.cabal.internal.util;

import java.io.InputStream;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CabalParserTest
    extends TestSupport
{
  private CabalParser underTest;

  @Before
  public void setup() throws Exception {
    this.underTest = new CabalParser();
  }

  @Test
  public void testParseCabalFile() throws Exception {
    InputStream is = getClass().getResourceAsStream("AlgoRhythm.cabal");
    Map<String, Object> cabalAttributes = underTest.loadAttributes(is);

    assertThat(cabalAttributes.get("name"), is(equalTo("AlgoRhythm")));
    assertThat(cabalAttributes.get("version"), is(equalTo("0.1.0.0")));
  }
}
