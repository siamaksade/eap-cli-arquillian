package com.jboss.cli.sample.datasource;

import static com.jboss.cli.sample.util.CliUtils.execute;
import static com.jboss.cli.sample.util.CliUtils.isSuccess;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.jboss.cli.sample.util.CliUtils;

@RunWith(Arquillian.class)
public class DataSourceTest {
	private static final String CLI_DATASOURCE_LIST = "/subsystem=datasources:read-children-names(child-type=data-source)";

	@Deployment
	public static JavaArchive createDeployment() {
		return ShrinkWrap.create(JavaArchive.class, "cli.jar")
				.addClass(CliUtils.class)
				.setManifest(new File("src/test/resources/META-INF/MANIFEST.MF"));
	}
	
	@Test
	public void addDataSource() {
		// run datasource_add.cli
		File file = new File("src/main/resources/datasource_add.cli");
		String output = execute(file, true);
		
		// verify successful command execution
		assertTrue(output, isSuccess(output));
		
		// verify ds added
		assertTrue(output, dataSourceExists("TestDS"));
	}
	
	@Test
	public void removeDataSource() {
		// run datasource_remove.cli
		File file = new File("src/main/resources/datasource_remove.cli");
		String output = execute(file, true);
		
		// verify successful command execution
		assertTrue(output, isSuccess(output));
		
		// verify ds removed
		assertFalse(output, dataSourceExists("TestDS"));
	}
	
	private boolean dataSourceExists(String ds) {
		final String output = CliUtils.execute(CLI_DATASOURCE_LIST, true);
		final List<ModelNode> nodes = CliUtils.getResultObject(output).asList();
		
		for (ModelNode node : nodes) {
			if (ds.equals(node.asString())) {
				return true;
			}
		}
		
		return false;
	}
}
