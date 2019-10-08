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
package org.sonatype.nexus.plugins.cabal.internal.proxy;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.plugins.cabal.internal.AssetKind;
import org.sonatype.nexus.plugins.cabal.internal.util.CabalDataAccess;
import org.sonatype.nexus.plugins.cabal.internal.util.CabalParser;
import org.sonatype.nexus.plugins.cabal.internal.util.CabalPathUtils;
import org.sonatype.nexus.plugins.cabal.internal.util.TgzParser;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Cabal {@link ProxyFacet} implementation.
 *
 * @since 0.0.1
 */
@Named
public class CabalProxyFacetImpl
    extends ProxyFacetSupport
    implements CabalProxyFacet
{
  private CabalPathUtils cabalPathUtils;

  private CabalDataAccess cabalDataAccess;

  private CabalParser cabalParser;

  private TgzParser tgzParser;

  @Inject
  public CabalProxyFacetImpl(final CabalPathUtils cabalPathUtils,
                             final CabalDataAccess cabalDataAccess,
                             final CabalParser cabalParser,
                             final TgzParser tgzParser)
  {
    this.cabalPathUtils = checkNotNull(cabalPathUtils);
    this.cabalDataAccess = checkNotNull(cabalDataAccess);
    this.cabalParser = checkNotNull(cabalParser);
    this.tgzParser = checkNotNull(tgzParser);
  }

  // HACK: Workaround for known CGLIB issue, forces an Import-Package for org.sonatype.nexus.repository.config
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    super.doValidate(configuration);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State matcherState;
    switch (assetKind) {
      case ARCHIVE:
        matcherState = cabalPathUtils.matcherState(context);
        return getAsset(cabalPathUtils.buildAssetPath(matcherState));
      case CABAL:
        matcherState = cabalPathUtils.matcherState(context);
        return getAsset(cabalPathUtils.buildCabalAssetPath(matcherState));
      case INCREMENTAL_PACKAGE_INDEX:
        matcherState = cabalPathUtils.matcherState(context);
        return getAsset(cabalPathUtils.buildIncrementalAssetPath(matcherState));
      case TIMESTAMP:
        return getAsset("timestamp.json");
      case SNAPSHOT:
        return getAsset("snapshot.json");
      case MIRRORS:
        return getAsset("mirrors.json");
      case ROOT:
        return getAsset("root.json");
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  @TransactionalTouchBlob
  public Content getAsset(final String assetPath) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = cabalDataAccess.findAsset(tx, tx.findBucket(getRepository()), assetPath);
    if (asset == null) {
      return null;
    }
    return cabalDataAccess.toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State matcherState;
    switch (assetKind) {
      case ARCHIVE:
        matcherState = cabalPathUtils.matcherState(context);
        return putCabalPackage(content,
            assetKind,
            cabalPathUtils.buildAssetPath(matcherState));
      case CABAL:
        matcherState = cabalPathUtils.matcherState(context);
        return putMetadata(content,
            assetKind,
            cabalPathUtils.buildCabalAssetPath(matcherState));
      case INCREMENTAL_PACKAGE_INDEX:
        matcherState = cabalPathUtils.matcherState(context);
        return putMetadata(content,
            assetKind,
            cabalPathUtils.buildIncrementalAssetPath(matcherState));
      case TIMESTAMP:
        return putMetadata(content,
            assetKind,
            "timestamp.json");
      case SNAPSHOT:
        return putMetadata(content,
            assetKind,
            "snapshot.json");
      case MIRRORS:
        return putMetadata(content,
            assetKind,
            "mirrors.json");
      case ROOT:
        return putMetadata(content,
            assetKind,
            "root.json");
      default:
        throw new IllegalStateException("Received an invalid AssetKind of type: " + assetKind.name());
    }
  }

  private Content putCabalPackage(final Content content,
                                  final AssetKind assetKind,
                                  final String assetPath)
      throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);

    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), CabalDataAccess.HASH_ALGORITHMS)) {
      Map<String, Object> attributes = cabalParser.loadAttributes(tgzParser.getCabalFileFromTempBlob(tempBlob.get()));
      Component component = findOrCreateComponent(assetPath, attributes);

      return findOrCreateAsset(tempBlob, content, assetKind, assetPath, component);
    }
  }

  @TransactionalStoreBlob
  protected Component findOrCreateComponent(final String assetPath, final Map<String, Object> attributes) {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Component component = cabalDataAccess.findComponent(tx,
        getRepository(),
        assetPath);

    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat())
          .name(attributes.get("name").toString())
          .version(attributes.get("version").toString());
    }
    tx.saveComponent(component);

    return component;
  }

  private Content putMetadata(final Content content,
                              final AssetKind assetKind,
                              final String assetPath) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);

    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), CabalDataAccess.HASH_ALGORITHMS)) {
      return findOrCreateAsset(tempBlob, content, assetKind, assetPath, null);
    }
  }

  @TransactionalStoreBlob
  protected Content findOrCreateAsset(final TempBlob tempBlob,
                                      final Content content,
                                      final AssetKind assetKind,
                                      final String assetPath,
                                      final Component component) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset asset = cabalDataAccess.findAsset(tx, bucket, assetPath);

    if (assetKind.equals(AssetKind.ARCHIVE)) {
      if (asset == null) {
        asset = tx.createAsset(bucket, component);
        asset.name(assetPath);
        asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
      }
    } else {
      if (asset == null) {
        asset = tx.createAsset(bucket, getRepository().getFormat());
        asset.name(assetPath);
        asset.formatAttributes().set(P_ASSET_KIND, assetKind.name());
      }
    }

    return cabalDataAccess.saveAsset(tx, asset, tempBlob, content);
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
      throws IOException
  {
    setCacheInfo(content, cacheInfo);
  }

  @TransactionalTouchMetadata
  public void setCacheInfo(final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug(
          "Attempting to set cache info for non-existent Cabal asset {}", content.getAttributes().require(Asset.class)
      );
      return;
    }
    log.debug("Updating cacheInfo of {} to {}", asset, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1);
  }
}
