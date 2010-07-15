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
package org.jboss.jca.common.metadata.ra.ra16;

/**
 * @author <a href="mailto:stefano.maestri@jboss.org">Stefano Maestri</a>
 *
 */
public class MessageListener implements IdDecoratedMetadata
{
   /**
    */
   private static final long serialVersionUID = 6417807206318228618L;

   private final String messagelistenerType;

   private final Activationspec activationspec;

   private final String id;

   /**
    * @param messagelistenerType full qualified name of the java type
    * @param activationspec  The activationspecType specifies an activation
    * specification.  The information includes fully qualified
    * Java class name of an activation specification and a set of
    * required configuration property names.
    * @param id XML ID
    */
   public MessageListener(String messagelistenerType, Activationspec activationspec, String id)
   {
      super();
      this.messagelistenerType = messagelistenerType;
      this.activationspec = activationspec;
      this.id = id;
   }

   /**
    * @return messagelistenerType
    */
   public String getMessagelistenerType()
   {
      return messagelistenerType;
   }

   /**
    * @return activationspec
    */
   public Activationspec getActivationspec()
   {
      return activationspec;
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
      result = prime * result + ((activationspec == null) ? 0 : activationspec.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((messagelistenerType == null) ? 0 : messagelistenerType.hashCode());
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
      if (!(obj instanceof MessageListener))
      {
         return false;
      }
      MessageListener other = (MessageListener) obj;
      if (activationspec == null)
      {
         if (other.activationspec != null)
         {
            return false;
         }
      }
      else if (!activationspec.equals(other.activationspec))
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
      if (messagelistenerType == null)
      {
         if (other.messagelistenerType != null)
         {
            return false;
         }
      }
      else if (!messagelistenerType.equals(other.messagelistenerType))
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
      return "MessageListener [messagelistenerType=" + messagelistenerType + ", activationspec=" + activationspec
            + ", id=" + id + "]";
   }
}
