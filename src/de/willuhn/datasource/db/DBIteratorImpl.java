/**********************************************************************
 * $Source: /cvsroot/jameica/datasource/src/de/willuhn/datasource/db/DBIteratorImpl.java,v $
 * $Revision: 1.9 $
 * $Date: 2004/06/17 00:05:50 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by  bbv AG
 * All rights reserved
 *
 **********************************************************************/
package de.willuhn.datasource.db;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.datasource.rmi.DBObject;
import de.willuhn.datasource.rmi.DBService;
import de.willuhn.datasource.rmi.GenericObject;

/**
 * @author willuhn
 * Kleiner Hilfsiterator zum Holen von Listen von Objekten aus der Datenbank.
 */
public class DBIteratorImpl extends UnicastRemoteObject implements DBIterator {

	private DBService service;
	private Connection conn;
	private AbstractDBObject object;
	private ArrayList list = new ArrayList();
	private int index = 0;
  private String filter = "";
  private String order = "";
  private boolean initialized = false;

  /**
   * Erzeugt einen neuen Iterator.
   * @param object Objekt, fuer welches die Liste erzeugt werden soll.
   * @param service der Datenbankservice.
   * @throws RemoteException
   */
  DBIteratorImpl(AbstractDBObject object, DBService service) throws RemoteException
	{
		if (object == null)
			throw new RemoteException("given object type is null");

  	this.object = object;
		this.service = service;
		this.conn = service.getConnection();

		if (conn == null)
			throw new RemoteException("given connection is null");

  }

  /**
   * Erzeugt einen neuen Iterator mit der uebergebenen Liste von IDs.
   * @param object Objekt, fuer welches die Liste erzeugt werden soll.
   * @param list eine vorgefertigte Liste.
   * @param service der Datenbank-Service.
   * @throws RemoteException
   */
  public DBIteratorImpl(AbstractDBObject object, ArrayList list, DBService service) throws RemoteException
  {
		this(object,service);

    if (list == null)
      throw new RemoteException("given list is null");

    try {
      for (int i=0;i<list.size();++i)
      {
        DBObject o = (DBObject) service.createObject(object.getClass(),(String)list.get(i));
        this.list.add(o);
      }
    }
    catch (Exception e)
    {
      throw new RemoteException("unable to create list",e);
    }
    this.initialized = true;
  }

  /**
   * @see de.willuhn.datasource.rmi.DBIterator#setOrder(java.lang.String)
   */
  public void setOrder(String order) throws RemoteException {
    if (this.initialized)
      return; // allready initialized

    this.order = " " + order;
  }

  /**
   * @see de.willuhn.datasource.rmi.DBIterator#addFilter(java.lang.String)
   */
  public void addFilter(String filter) throws RemoteException {
    if (this.initialized)
      return; // allready initialized

    if (filter == null)
      return; // no filter given

    if ("".equals(this.filter))
    {
      this.filter = filter;
    }
    else {
      this.filter += " and " + filter;
    }

  }

  /**
   * Baut das SQL-Statement fuer die Liste zusammen.
   * @return das erzeugte Statement.
   * @throws RemoteException
   */
  private String prepareSQL() throws RemoteException
  {
    String sql = object.getListQuery();

    // mhh, da steht schon eine "where" klausel drin
    if (sql.indexOf(" where ") != -1)
    {
      // also fuegen wir den Filter via "and" hinten dran. Aber nur, wenn auch einer da ist ;)
      if (!"".equals(this.filter))
        sql += " and " + filter;
    }
    else if (filter != null && !"".equals(filter))
    {
      // ansonsten pappen wir den Filter so hinten dran, wie er kommt
      sql += " where " + filter;
    }

    // Statement enthaelt noch kein Order - also koennen wir unseres noch dranschreiben
    if (sql.indexOf(" order ") == -1)
    {
      sql += order;
    }
    return sql;
  }

  /**
   * Initialisiert den Iterator.
   * @throws RemoteException
   */
  private void init() throws RemoteException {
    if (this.initialized)
      return; // allready initialzed

		Statement stmt = null;
    String sql = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
      sql = prepareSQL();
      
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
        DBObject o = (DBObject) service.createObject(object.getClass(),rs.getString(object.getIDField()));
				list.add(o);
			}
      this.initialized = true;
		}
		catch (Exception e)
		{
			throw new RemoteException("unable to init iterator",e);
		}
		finally {
			try {
				rs.close();
				stmt.close();
			} catch (Exception se) {/*useless*/}
		}
	}

  /**
   * @see de.willuhn.datasource.rmi.GenericIterator#hasNext()
   */
  public boolean hasNext() throws RemoteException
	{
    if (!initialized) init();
		return (index < list.size() && list.size() > 0);
	}

  /**
   * @see de.willuhn.datasource.rmi.GenericIterator#next()
   */
  public GenericObject next() throws RemoteException
	{
    if (!initialized) init();
    try {
      return (GenericObject) list.get(index++);
    }
    catch (Exception e)
    {
      throw new RemoteException(e.getMessage());
    }
	}
  
  /**
   * @see de.willuhn.datasource.rmi.GenericIterator#previous()
   */
  public GenericObject previous() throws RemoteException
  {
    if (!initialized) init();
    try {
      return (GenericObject) list.get(index--);
    }
    catch (Exception e)
    {
      throw new RemoteException(e.getMessage());
    }
  }

  /**
   * @see de.willuhn.datasource.rmi.GenericIterator#size()
   */
  public int size() throws RemoteException
  {
    if (!initialized) init();
    return list.size();
  }

  /**
   * @see de.willuhn.datasource.rmi.GenericIterator#begin()
   */
  public void begin() throws RemoteException
  {
    this.index = 0;
  }

  /**
   * @see de.willuhn.datasource.rmi.GenericIterator#contains(de.willuhn.datasource.rmi.GenericObject)
   */
  public GenericObject contains(GenericObject other) throws RemoteException
  {
    if (!initialized) init();

    if (other == null)
      return null;

    if (!other.getClass().equals(object.getClass()))
      return null; // wir koennen uns die Iteration sparen.

    GenericObject object = null;
    for (int i=0;i<list.size();++i)
    {
      object = (GenericObject) list.get(i);
      if (object.equals(other))
        return object;
    }
    
    return null;
    
  }

  /**
   * Liefert den aktuellen Typ der Iteration.
   * @return Klasse der Iterations-Elemente.
   * @throws RemoteException
   */
  public Class getType() throws RemoteException {
    return object.getClass();
  }
}


/*********************************************************************
 * $Log: DBIteratorImpl.java,v $
 * Revision 1.9  2004/06/17 00:05:50  willuhn
 * @N GenericObject, GenericIterator
 *
 * Revision 1.8  2004/06/10 20:22:40  willuhn
 * @D javadoc comments fixed
 *
 * Revision 1.7  2004/04/05 23:28:30  willuhn
 * *** empty log message ***
 *
 * Revision 1.6  2004/03/29 20:36:23  willuhn
 * *** empty log message ***
 *
 * Revision 1.5  2004/03/18 01:24:17  willuhn
 * @C refactoring
 *
 * Revision 1.4  2004/03/06 18:24:34  willuhn
 * @D javadoc
 *
 * Revision 1.3  2004/02/23 20:31:26  willuhn
 * @C refactoring in AbstractDialog
 *
 * Revision 1.2  2004/01/23 00:25:52  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2004/01/10 14:52:19  willuhn
 * @C package removings
 *
 * Revision 1.1  2004/01/08 20:46:44  willuhn
 * @N database stuff separated from jameica
 *
 * Revision 1.19  2004/01/03 18:08:05  willuhn
 * @N Exception logging
 * @C replaced bb.util xml parser with nanoxml
 *
 * Revision 1.18  2003/12/29 16:29:47  willuhn
 * @N javadoc
 *
 * Revision 1.17  2003/12/28 22:58:27  willuhn
 * @N synchronize mode
 *
 * Revision 1.16  2003/12/19 19:45:02  willuhn
 * *** empty log message ***
 *
 * Revision 1.15  2003/12/19 01:43:26  willuhn
 * @N added Tree
 *
 * Revision 1.14  2003/12/18 21:47:12  willuhn
 * @N AbstractDBObjectNode
 *
 * Revision 1.13  2003/12/16 02:27:44  willuhn
 * *** empty log message ***
 *
 * Revision 1.12  2003/12/11 21:00:54  willuhn
 * @C refactoring
 *
 * Revision 1.11  2003/12/10 23:51:55  willuhn
 * *** empty log message ***
 *
 * Revision 1.10  2003/12/10 01:12:55  willuhn
 * *** empty log message ***
 *
 * Revision 1.9  2003/12/10 00:47:12  willuhn
 * @N SearchDialog done
 * @N ErrorView
 *
 * Revision 1.8  2003/12/05 17:12:23  willuhn
 * @C SelectInput
 *
 * Revision 1.7  2003/12/01 23:02:00  willuhn
 * *** empty log message ***
 *
 * Revision 1.6  2003/12/01 20:28:58  willuhn
 * @B filter in DBIteratorImpl
 * @N InputFelder generalisiert
 *
 * Revision 1.5  2003/11/30 16:23:09  willuhn
 * *** empty log message ***
 *
 * Revision 1.4  2003/11/22 20:43:05  willuhn
 * *** empty log message ***
 *
 * Revision 1.3  2003/11/21 02:10:21  willuhn
 * @N prepared Statements in AbstractDBObject
 * @N a lot of new SWT parts
 *
 * Revision 1.2  2003/11/20 03:48:42  willuhn
 * @N first dialogues
 *
 * Revision 1.1  2003/11/05 22:46:19  willuhn
 * *** empty log message ***
 *
 * Revision 1.3  2003/10/29 20:56:49  willuhn
 * @N added transactionRollback
 *
 * Revision 1.2  2003/10/29 17:33:22  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2003/10/27 11:49:12  willuhn
 * @N added DBIterator
 *
 **********************************************************************/