/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.jca.common.api.metadata.ra;



import org.jboss.jca.common.api.metadata.CopyUtil;
import org.jboss.jca.common.api.metadata.CopyableMetaData;
import org.jboss.jca.common.api.metadata.MergeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:stefano.maestri@jboss.org">Stefano Maestri</a>
 *
 */
public class LicenseType implements IdDecoratedMetadata, MergeableMetadata<LicenseType>, CopyableMetaData
{

   /**
    */
   private static final long serialVersionUID = 1590514246054447090L;

   private final ArrayList<LocalizedXsdString> description;

   private final boolean licenseRequired;

   private final String id;

   /**
    * @param description description of the license
    * @param licenseRequired mandatory boolena value
    * @param id XML ID
    */
   public LicenseType(List<LocalizedXsdString> description, boolean licenseRequired, String id)
   {
      super();
      if (description != null)
      {
         this.description = new ArrayList<LocalizedXsdString>(description.size());
         this.description.addAll(description);
      }
      else
      {
         this.description = new ArrayList<LocalizedXsdString>(0);
      }
      this.licenseRequired = licenseRequired;
      this.id = id;
   }

   /**
    * @return description
    */
   public List<LocalizedXsdString> getDescriptions()
   {
      return description == null ? null : Collections.unmodifiableList(description);
   }

   /**
    * @return licenseRequired
    */
   public boolean isLicenseRequired()
   {
      return licenseRequired;
   }

   /**
    * {@inheritDoc}
    *
    * @see IdDecoratedMetadata#getId()
    */
   @Override
   public String getId()
   {
      return id;
   }

   /**
    * {@inheritDoc}
    *
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((description == null) ? 0 : description.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + (licenseRequired ? 1231 : 1237);
      return result;
   }

   /**
    * {@inheritDoc}
    *
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj == null)
      {
         return false;
      }
      if (!(obj instanceof LicenseType))
      {
         return false;
      }
      LicenseType other = (LicenseType) obj;
      if (description == null)
      {
         if (other.description != null)
         {
            return false;
         }
      }
      else if (!description.equals(other.description))
      {
         return false;
      }
      if (id == null)
      {
         if (other.id != null)
         {
            return false;
         }
      }
      else if (!id.equals(other.id))
      {
         return false;
      }
      if (licenseRequired != other.licenseRequired)
      {
         return false;
      }
      return true;
   }

   /**
    * {@inheritDoc}
    *
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();

      sb.append("<license");
      if (id != null)
         sb.append(" ").append(Attribute.ID).append("=\"").append(id).append("\"");
      sb.append(">");

      if (description != null)
      {
         for (LocalizedXsdString s : description)
         {
            sb.append("<").append(Tag.DESCRIPTION).append(">");
            sb.append(s);
            sb.append("</").append(Tag.DESCRIPTION).append(">");            
         }
      }

      sb.append("<").append(Tag.LICENSE_REQUIRED).append(">");
      sb.append(licenseRequired);
      sb.append("</").append(Tag.LICENSE_REQUIRED).append(">");

      sb.append("</license>");

      return sb.toString();
   }

   /**
    *
    * A Tag.
    *
    * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
    *
    */
   public enum Tag
   {
      /** always first
       *
       */
      UNKNOWN(null),

      /**
       * description tag
       */
      DESCRIPTION("description"),

      /**
       * vendor-name tag
       */
      LICENSE_REQUIRED("license-required");

      private final String name;

      /**
       *
       * Create a new Tag.
       *
       * @param name a name
       */
      Tag(final String name)
      {
         this.name = name;
      }

      /**
       * Get the local name of this element.
       *
       * @return the local name
       */
      public String getLocalName()
      {
         return name;
      }

      /**
       * {@inheritDoc}
       */
      public String toString()
      {
         return name;
      }

      private static final Map<String, Tag> MAP;

      static
      {
         final Map<String, Tag> map = new HashMap<String, Tag>();
         for (Tag element : values())
         {
            final String name = element.getLocalName();
            if (name != null)
               map.put(name, element);
         }
         MAP = map;
      }

      /**
      *
      * Static method to get enum instance given localName string
      *
      * @param localName a string used as localname (typically tag name as defined in xsd)
      * @return the enum instance
      */
      public static Tag forName(String localName)
      {
         final Tag element = MAP.get(localName);
         return element == null ? UNKNOWN : element;
      }

   }

   /**
    *
    * A Attribute.
    *
    * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
    *
    */
   public enum Attribute
   {

      /** id attribute
       *
       */
      ID("id");

      private final String name;

      /**
       *
       * Create a new Tag.
       *
       * @param name a name
       */
      Attribute(final String name)
      {
         this.name = name;
      }

      /**
       * Get the local name of this element.
       *
       * @return the local name
       */
      public String getLocalName()
      {
         return name;
      }

      /**
       * {@inheritDoc}
       */
      public String toString()
      {
         return name;
      }
   }

   @Override
   public LicenseType merge(MergeableMetadata<?> jmd) throws Exception
   {
      if (jmd instanceof LicenseType)
      {
         LicenseType inputLicense = (LicenseType) jmd;

         boolean newLicenseRequired = this.licenseRequired || inputLicense.licenseRequired;
         String newId = this.id == null ? inputLicense.id : this.id;
         List<LocalizedXsdString> newDescription = MergeUtil.mergeList(this.description,
               inputLicense.description);
         return new LicenseType(newDescription, newLicenseRequired, newId);
      }
      else
      {
         return this;
      }
   }

   @Override
   public CopyableMetaData copy()
   {
      return new LicenseType(CopyUtil.cloneList(description), licenseRequired, CopyUtil.cloneString(id));
   }

}
