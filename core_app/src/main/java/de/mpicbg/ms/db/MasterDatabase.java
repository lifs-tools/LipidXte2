package de.mpicbg.ms.db;

import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.data.FAAnionRow;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/**
  * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
  * Organization: MPI-CBG Dresden
  * Date: November 2016
  */
 public class MasterDatabase
 {
	 Optional< Connection > conn = Optional.empty();

	 public MasterDatabase()
	 {

	 }

	 public boolean connect()
	 {
		 try
		 {
			 Class.forName( "org.sqlite.JDBC" );
		 }
		 catch ( ClassNotFoundException e )
		 {
			 System.err.println( e.getMessage() );
		 }

		 if ( !conn.isPresent() )
		 {
			 try
			 {
				 conn = Optional.of( DriverManager.getConnection( "jdbc:sqlite:" + System.getProperty("user.home") + "/LipidXteSqlite.db", "sa", "" ) );
			 }
			 catch ( SQLException e )
			 {
				 System.err.println( e.getMessage() );
			 }
		 }

		 boolean ret = false;

		 if( !conn.isPresent() )
			 return ret;

		 try
		 {
			 ret = !conn.get().isClosed();
		 }
		 catch ( SQLException e )
		 {
			 System.err.println( e.getMessage() );
//			 e.printStackTrace();
		 }

		 return ret;
	 }

	 public void close()
	 {
		 if ( conn.isPresent() )
		 {
			 try
			 {
				 Class.forName( "org.sqlite.JDBC" );
			 }
			 catch ( ClassNotFoundException e )
			 {
				 System.err.println( e.getMessage() );
			 }

			 try
			 {
				 conn.get().close();
			 }
			 catch ( SQLException e )
			 {
				 System.err.println( e.getMessage() );
			 }
		 }
	 }

//	 public void insert(Calib calib, String group, int index, float mz, float snFactor, float co2mz)
	public void insert( String group, int index, float mz, int iso, float sn1, float sn2, float sym )
	{
		if ( conn.isPresent() )
		{
			try
			{
				Statement stat = conn.get().createStatement();

				DecimalFormat df = new DecimalFormat("###.###");

				String sql = String.format( "INSERT OR REPLACE INTO MASTER (GRP, MZ, REF, ISO, SN1, SN2, SYM) VALUES('%s', %s, %d, %d, %.0f, %.0f, %.0f)",
						group, df.format( mz ), index, iso, sn1, sn2, sym );

//				System.out.println( sql );

				stat.execute( sql );

				stat.close();
			}
			catch ( SQLException e )
			{
				System.err.println( e.getMessage() );
//				e.printStackTrace();
			}
		}
	}

	 public void insertMasterData(String ref, String group, String mz, String iso, String sn1, String sn2, String sym, String co2Mz)
	 {
		 if ( conn.isPresent() )
		 {
			 try
			 {
				 Statement stat = conn.get().createStatement();

				 String sql = String.format( "INSERT OR REPLACE INTO MASTER (REF, GRP, MZ, ISO, SN1, SN2, SYM, CO2MZ) VALUES(%s, '%s', %s, %s, %s, %s, %s, %s)",
						 ref, group, mz, iso, sn1, sn2, sym, co2Mz );

//				 System.out.println( sql );

				 stat.execute( sql );

				 stat.close();
			 }
			 catch ( SQLException e )
			 {
				 System.err.println( e.getMessage() );
//				 e.printStackTrace();
			 }
		 }
	 }

	 public void insertMasterData(String ref, String group, String mz, String iso, String sn1, String sn2, String sym)
	 {
		 if ( conn.isPresent() )
		 {
			 try
			 {
				 Statement stat = conn.get().createStatement();

				 String sql = String.format( "INSERT OR REPLACE INTO MASTER (REF, GRP, MZ, ISO, SN1, SN2, SYM) VALUES(%s, '%s', %s, %s, %s, %s, %s)",
						 ref, group, mz, iso, sn1, sn2, sym );

//				 System.out.println( sql );

				 stat.execute( sql );

				 stat.close();
			 }
			 catch ( SQLException e )
			 {
				 System.err.println( e.getMessage() );
//				 e.printStackTrace();
			 }
		 }
	 }

	 public void insertDetailData(String ref, String group, String sn1, String sn2, String sym, List<String[]> rows)
	 {
		 if ( conn.isPresent() )
		 {
			 try
			 {
				 Statement stat = conn.get().createStatement();

				 for(String[] row : rows)
				 {
					 String sql;

					 if( row[3].equals( "" ) )
					 	sql = String.format( "INSERT OR REPLACE INTO DETAIL (REF, GRP, SN1, SN2, SYM, CE, INT, CF) VALUES(%s, '%s', %s, %s, %s, %s, %s, %s)",
							 ref, group, sn1, sn2, sym, row[0], row[1], row[2] );
					 else
						sql = String.format( "INSERT OR REPLACE INTO DETAIL (REF, GRP, SN1, SN2, SYM, CE, INT, CF, CO2INT) VALUES(%s, '%s', %s, %s, %s, %s, %s, %s, %s)",
								 ref, group, sn1, sn2, sym, row[0], row[1], row[2], row[3] );

//					 System.out.println( sql );

					 stat.execute( sql );
				 }

				 stat.close();
			 }
			 catch ( SQLException e )
			 {
				 System.err.println( e.getMessage() );
//				 e.printStackTrace();
			 }
		 }
	 }

	 public void initTables( boolean bNew )
	 {
		 if ( conn.isPresent() )
		 {
			 try
			 {
				 Statement stat = conn.get().createStatement();

				 if( bNew )
				 {
					 stat.execute( "DROP TABLE IF EXISTS DETAIL;" );

					 stat.execute( "DROP TABLE IF EXISTS MASTER;" );
				 }

				 stat.execute( "CREATE TABLE IF NOT EXISTS MASTER"
								 + "("
								 + "  REF INTEGER NOT NULL,"
								 + "  GRP VARCHAR(5) NOT NULL,"
								 + "  MZ DOUBLE NOT NULL,"
								 + "  ISO DOUBLE NOT NULL,"
								 + "  SN1 DOUBLE NOT NULL,"
								 + "  SN2 DOUBLE NOT NULL,"
								 + "  SYM DOUBLE NOT NULL,"
								 + "  CO2MZ DOUBLE,"
								 + "  PRIMARY KEY (REF, GRP, SN1, SN2, SYM)"
								 + ");"
				 );

				 stat.execute( "CREATE TABLE IF NOT EXISTS DETAIL"
								 + "("
								 + "  REF INTEGER NOT NULL,"
						 		 + "  GRP VARCHAR(5) NOT NULL,"
								 + "  SN1 DOUBLE NOT NULL,"
								 + "  SN2 DOUBLE NOT NULL,"
								 + "  SYM DOUBLE NOT NULL,"
								 + "  CE DOUBLE NOT NULL,"
								 + "  INT DOUBLE,"
								 + "  CF DOUBLE,"
								 + "  CO2INT DOUBLE,"
								 + "  PRIMARY KEY (REF, GRP, SN1, SN2, SYM, CE),"
								 + "  FOREIGN KEY (REF, GRP, SN1, SN2, SYM) REFERENCES MASTER (REF, GRP, SN1, SN2, SYM)"
								 + ");"
				 );

				 stat.close();
			 }
			 catch ( SQLException e )
			 {
				 e.printStackTrace();
			 }
		 }
	 }

	 public TreeMap<String, TreeMap< Integer, FAAnion > > getMasterDB()
	 {
		 if ( conn.isPresent() )
		 {
			 TreeMap<String, TreeMap< Integer, FAAnion > > masterDB = new TreeMap<>(  );

			 try
			 {
				 Statement stat = conn.get().createStatement();
				 ResultSet rs;
				 rs = stat.executeQuery( "SELECT DISTINCT GRP, REF, MZ, ISO from master order by GRP, REF ASC" );
				 while ( rs.next() )
				 {
					 String grp = rs.getString( "GRP" );
					 if( !masterDB.containsKey( grp ) )
						 masterDB.put( grp, new TreeMap<>(  ) );

					 masterDB.get(grp).put( rs.getInt( "REF" ), new FAAnion( rs.getInt( "REF" ), rs.getDouble( "MZ" ), rs.getFloat( "ISO" ) ) );
				 }
				 stat.close();
			 }
			 catch ( SQLException e )
			 {
				 e.printStackTrace();
			 }

			 return masterDB;
		 }
		 else
		  return null;
	 }

	 public List<FAAnionRow> getFAAnionRows( String grp, Integer ref )
	 {
		 ArrayList<FAAnionRow> list = new ArrayList<>(  );

		 if ( conn.isPresent() )
		 {
			 try
			 {
				 Statement stat = conn.get().createStatement();
				 ResultSet rs;
				 rs = stat.executeQuery( "SELECT MZ, ISO, SN1, SN2, SYM, CO2MZ FROM master WHERE REF=" + ref + " AND GRP='" + grp +"'" );
				 while ( rs.next() )
				 {
					 FAAnionRow faAnionRow = new FAAnionRow( ref, rs.getDouble( "MZ" ), rs.getFloat( "ISO" )  );
					 faAnionRow.setMaster( true );

					 float val = rs.getFloat( "SN1" );
					 if( val != 0f ) faAnionRow.getSn1().setName( val + "" );

					 val = rs.getFloat( "SN2" );
					 if( val != 0f ) faAnionRow.getSn2().setName( val + "" );

					 val = rs.getFloat( "SYM" );
					 if( val != 0f ) faAnionRow.getSym().setName( val + "" );

					 double co2mz = rs.getDouble( "CO2MZ" );
					 if( co2mz != 0d )
					 {
						 faAnionRow.setCo2MassString( co2mz + "" );
					 }

					 list.add( faAnionRow );
				 }
				 stat.close();
			 }
			 catch ( SQLException e )
			 {
				 e.printStackTrace();
			 }
		 }

		 return list;
	 }

	 public List<String[]> getDetails( Integer ref, String clazz, String sn1, String sn2, String sym )
	 {
		 ArrayList<String[]> list = new ArrayList<>(  );

		 if ( conn.isPresent() )
		 {
			 try
			 {
				 Statement stat = conn.get().createStatement();
				 ResultSet rs;
				 rs = stat.executeQuery( "SELECT CE, INT, CF, CO2INT FROM detail WHERE REF=" + ref +
						 " AND GRP = '" + clazz + "' AND SN1=" + sn1 + " AND SN2=" + sn2 + " AND SYM=" + sym );
				 while ( rs.next() )
				 {
					 double co2mz = rs.getDouble( "CO2INT" );
					 if( co2mz == 0d )
					 {
						 list.add( new String[]{
								 rs.getString( "CE" ), rs.getString( "INT" ), rs.getString( "CF" ), "", ""
						 } );
					 }
					 else
					 {
						 list.add( new String[]{
								 rs.getString( "CE" ), rs.getString( "INT" ), rs.getString( "CF" ),
								 rs.getString( "CO2INT" )
						 } );
					 }
				 }
				 stat.close();
			 }
			 catch ( SQLException e )
			 {
				 e.printStackTrace();
			 }
		 }

		 return list;
	 }

    public String[] getDetail( Float ce, Integer ref, String clazz, String sn1, String sn2, String sym )
    {
       String[] ret = null;

       if ( conn.isPresent() )
       {
          try
          {
             Statement stat = conn.get().createStatement();
             ResultSet rs;
             rs = stat.executeQuery( "SELECT CE, INT, CF, CO2INT FROM detail WHERE REF=" + ref +
                     " AND GRP = '" + clazz + "' AND SN1=" + sn1 + " AND SN2=" + sn2 + " AND SYM=" + sym + " AND CE=" + ce );
             while ( rs.next() )
             {
                double co2mz = rs.getDouble( "CO2INT" );
                if( co2mz == 0d )
                {
                   ret = new String[]{
                           rs.getString( "CE" ), rs.getString( "INT" ), rs.getString( "CF" ), "", ""
                   };
                }
                else
                {
                   ret = new String[]{
                           rs.getString( "CE" ), rs.getString( "INT" ), rs.getString( "CF" ),
                           rs.getString( "CO2INT" )
                   };
                }
             }
             stat.close();
          }
          catch ( SQLException e )
          {
             e.printStackTrace();
          }
       }

       return ret;
    }

	 public void checkNames()
	 {
		 if ( conn.isPresent() )
		 {
			 try
			 {
				 Statement stat = conn.get().createStatement();
				 ResultSet rs;
				 rs = stat.executeQuery( "select * from MASTER" );
				 while ( rs.next() )
				 {
					 System.out.println( rs.getString( "REF" ) );
				 }
				 stat.close();
			 }
			 catch ( SQLException e )
			 {
				 e.printStackTrace();
			 }
		 }
	 }
 }
