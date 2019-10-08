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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class CabalParser
    extends ComponentSupport
{
  public Map<String, Object> loadAttributes(final InputStream is) throws IOException {
    checkNotNull(is);

    String data = IOUtils.toString(is);

    Map<String, Object> map = new HashMap<>();

    Scanner scanner = new Scanner(data);

    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      checkIfVersionOrName(line, map);
    }
    scanner.close();

    return map;
  }

  private void checkIfVersionOrName(final String scan, Map<String, Object> map) {
    if (scan.contains("name") || scan.contains("version")) {
      String newLine = scan.replaceAll("\\s", "");
      String[] results = newLine.split(":");
      map.put(results[0], results[1]);
    }
  }
}
