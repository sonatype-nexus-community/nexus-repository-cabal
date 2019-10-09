<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2019-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
[Cabal](https://www.haskell.org/cabal/) is a system for building and packaging Haskell libraries and programs.

Full documentation on installing `cabal` can be found on [the Cabal project website](https://www.haskell.org/cabal/#install-upgrade). 

You can create a proxy repository in Nexus Repository Manager (NXRM) that will cache packages from a remote Cabal repository, like
[Hackage](http://hackage.haskell.org/). Then, you can make the `cabal` client use your NXRM Proxy 
instead of the remote repository.
 
To proxy a Cabal repository, you simply create a new 'cabal (proxy)' as documented in 
[Repository Management](https://help.sonatype.com/repomanager3/configuration/repository-management) in
detail. Minimal configuration steps are:

- Define 'Name' - e.g. `cabal-proxy`
- Define URL for 'Remote storage' - e.g. [Hackage](http://hackage.haskell.org/)
- Select a 'Blob store' for 'Storage'

If you have not called `cabal update` before, you'll need to create a config file by running `cabal user-config update`. 
Edit your config file located at `~/.cabal/config` to use your NXRM Cabal Proxy, and change the `secure` flag to `False`, for example:
```
repository cabal-proxy
   url: http://localhost:8081/repository/cabal-proxy/
   -- secure: False
   -- root-keys:
   -- key-threshold: 3
```

Detailed Cabal package installation instructions can be found [on the Haskell wiki](https://wiki.haskell.org/Cabal-Install). If you want to do a quick test you can run the following commands:

    $ mkdir testproject && cd testproject
    $ cabal update
    $ cabal install titlecase
    
After making the `testproject` directory, the commands above tell `cabal` to fetch the index of packages from your NXRM Cabal proxy. The NXRM Cabal proxy will then download any missing packages from the remote Cabal repository, and cache the packages on the NXRM Cabal proxy.
The next time any client requests the same package from your NXRM Cabal proxy, the already cached package will
be returned to the client.
