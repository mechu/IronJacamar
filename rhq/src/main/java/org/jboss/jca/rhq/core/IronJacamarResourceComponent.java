/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.jca.rhq.core;

import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.rhq.util.ContainerHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;

/**
 * A IronJacamarResourceComponent
 * 
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a> 
 */
public class IronJacamarResourceComponent extends AbstractResourceComponent implements CreateChildResourceFacet
{
   
   /**
    * Maps contains DataSource URL and associated jndis mappings.
    */
   private Map<URL, List<String>> dsURLJndiMap = new HashMap<URL, List<String>>();

   /**
    * loadResourceConfiguration
    * 
    * @return Configuration the configuration
    * @throws Exception exception
    */
   @Override
   public Configuration loadResourceConfiguration() throws Exception
   {
      return new Configuration();
   }

   /**
    * createResource
    * 
    * @param report the CreateResourceReport
    * @return CreateResourceReport the report
    */
   @Override
   public CreateResourceReport createResource(CreateResourceReport report)
   {
      ResourceType resType = report.getResourceType();
      String resName = resType.getName();
      ResourcePackageDetails pkgDetail = report.getPackageDetails();
      ContentContext contentContext = getResourceContext().getContentContext();
      ContentServices contentServices = contentContext.getContentServices();
      String tmpDir = getUploadedDir();
      PackageDetailsKey pkgKey = pkgDetail.getKey();
      String pkgTypeName = pkgKey.getPackageTypeName();
      String fileName = pkgDetail.getFileName();
      if (pkgTypeName.equals("rar-file") && !fileName.toLowerCase().endsWith(".rar"))
      {
         report.setErrorMessage(fileName + " is not a valid RAR file.");
         report.setStatus(CreateResourceStatus.FAILURE);
         return report;
      }
      else if (pkgTypeName.equals("ds-file") && !fileName.toLowerCase().endsWith("-ds.xml"))
      {
         report.setErrorMessage(fileName + " is not a valid DataSource file.");
         report.setStatus(CreateResourceStatus.FAILURE);
         return report;
      }
      File outFile = new File(tmpDir, fileName); // change to plugin configuration ??
      OutputStream output;
      try
      {
         output = new FileOutputStream(outFile);
         PackageDetailsKey key = pkgDetail.getKey();
         long bits = contentServices.downloadPackageBitsForChildResource(contentContext, resName, key, output);
         if (bits < 0)
         {
            report.setErrorMessage("Can not download package content.");
            report.setStatus(CreateResourceStatus.FAILURE);
            return report;
         }
         Deploy deployer = (Deploy)ContainerHelper.getEmbeddedDiscover();
         URL url = outFile.toURI().toURL();
         if (pkgTypeName.equals("ds-file"))
         {
            List<String> dsJndiNamesBeforeDeploy = this.getAllDataSourceJndiNames();
            deployer.deploy(url);
            List<String> dsJndiNamesAfterDeploy = this.getAllDataSourceJndiNames();
            List<String> moreJndiNames = moreDeployedDsJndis(dsJndiNamesBeforeDeploy, dsJndiNamesAfterDeploy);
            if (!moreJndiNames.isEmpty())
            {
               this.dsURLJndiMap.put(url, moreJndiNames);
            }
         }
         else
         {
            deployer.deploy(url);
         }
         String resKey = outFile.getName();
         
         // set resource key
         report.setResourceKey(resKey);
         
         // set resource name
         report.setResourceName(resKey);
         
         report.setStatus(CreateResourceStatus.SUCCESS);
      }
      catch (Throwable e)
      {
         e.printStackTrace();
         report.setStatus(CreateResourceStatus.FAILURE);
         report.setErrorMessage(e.getMessage());
         report.setException(e);
      }
      return report;
   }

   /**
    * Gets more DataSource jndis after a -ds.xml is deployed.
    * 
    * @param dsJndiNamesBeforeDeploy DataSource Jndis before the -ds.xml is deployed
    * @param dsJndiNamesAfterDeploy  DataSource Jndis after the -ds.xml is deployed
    * @return more DataSource Jndis.
    */
   private List<String> moreDeployedDsJndis(List<String> dsJndiNamesBeforeDeploy, List<String> dsJndiNamesAfterDeploy)
   {
      List<String> moreDsJndis = new ArrayList<String>();
      for (String ds : dsJndiNamesAfterDeploy)
      {
         if (!dsJndiNamesBeforeDeploy.contains(ds))
         {
            moreDsJndis.add(ds);
         }
      }
      return moreDsJndis;
   }

   /**
    * Gets all DataSource JndiNames in current ManagementRepository.
    * 
    * @return all DataSource JndiNames
    */
   private List<String> getAllDataSourceJndiNames()
   {
      List<String> dsJndiNames = new ArrayList<String>();
      ManagementRepository mr = ManagementRepositoryManager.getManagementRepository();
      for (DataSource ds : mr.getDataSources())
      {
         dsJndiNames.add(ds.getJndiName());
      }
      return dsJndiNames;
   }
   
   /**
    * Removes the DataSource entry from the dsURLJndiMap after the DataSource is undeployed.
    * 
    * @param dsJndiName DataSource JndiName
    * @return true if remove succeeds, false otherwise.
    * @throws Throwable the exception
    */
   public boolean unDeployDataSource(String dsJndiName) throws Throwable
   {
      URL dsURL = null;
      for (Map.Entry<URL, List<String>> dsEntry : this.dsURLJndiMap.entrySet())
      {
         if (dsEntry.getValue().contains(dsJndiName))
         {
            dsURL = dsEntry.getKey();
            break;
         }
      }
      if (dsURL != null)
      {
         Deploy deployer = (Deploy)ContainerHelper.getEmbeddedDiscover();
         deployer.undeploy(dsURL);
         this.dsURLJndiMap.remove(dsURL);
         return true;
      }
      return false;
   }
   
}
