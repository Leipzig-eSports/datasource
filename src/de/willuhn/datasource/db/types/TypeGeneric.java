/**********************************************************************
 * $Source: /cvsroot/jameica/datasource/src/de/willuhn/datasource/db/types/TypeGeneric.java,v $
 * $Revision: 1.1 $
 * $Date: 2008/07/11 09:30:17 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.datasource.db.types;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Generisch.
 */
public class TypeGeneric implements Type
{
  /**
   * @see de.willuhn.datasource.db.types.Type#get(java.sql.ResultSet, java.lang.String)
   */
  public Object get(ResultSet rs, String name) throws SQLException
  {
    return rs.getObject(name);
  }

  /**
   * @see de.willuhn.datasource.db.types.Type#set(java.sql.PreparedStatement, int, java.lang.Object)
   */
  public void set(PreparedStatement stmt, int index, Object value) throws SQLException
  {
    if (value == null)
      stmt.setNull(index,Types.NULL);
    else
      stmt.setObject(index,value);
  }
}


/*********************************************************************
 * $Log: TypeGeneric.java,v $
 * Revision 1.1  2008/07/11 09:30:17  willuhn
 * @N Support fuer Byte-Arrays
 * @N SQL-Typen sind jetzt erweiterbar
 *
 **********************************************************************/