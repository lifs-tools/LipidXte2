package de.mpicbg.ms.db;

import org.junit.Test;

import static org.junit.Assert.*;

public class MasterDatabaseTest
{
	@Test
	public void testConnect() throws Exception
	{
		MasterDatabase db = new MasterDatabase();
		assertEquals(true, db.connect());
		db.initTables( false );
		db.insert("PC", 3, 199.17f, 0, 1.0f, 0f, 0f);
		db.checkNames();
	}
}