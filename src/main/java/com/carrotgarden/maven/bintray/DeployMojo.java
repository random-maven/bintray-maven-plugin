package com.carrotgarden.maven.bintray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.deploy.AbstractDeployMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

/**
 * <p>
 * Deploy maven project artifacts to existing bintray maven repository.
 * <p>
 * Goal operates via bintray rest api:
 * <a href="https://bintray.com/docs/api/">https://bintray.com/docs/api/</a>.
 * <p>
 * Optional features:
 * <ul>
 * <li>create bintray repository package on-demand
 * <li>remove previous versions from bintray package
 * </ul>
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY, requiresProject = true)
public class DeployMojo extends AbstractDeployMojo {

	/**
	 * Bintray rest api url. Entry point to the bintray resource management.
	 */
	@Parameter(property = "bintray.restApiUrl", defaultValue = "https://bintray.com/api/v1")
	private String restApiUrl;

	/**
	 * Bintray user or organization name which contains target maven repository
	 * {@link #mavenRepo}.
	 */
	@Parameter(property = "bintray.subject", defaultValue = "${user.name}")
	private String subject;

	/**
	 * Bintray target maven repository name. Repository must already exist for the
	 * bintray {@link #subject}.
	 */
	@Parameter(property = "bintray.mavenRepo", defaultValue = "maven")
	private String mavenRepo;

	/**
	 * Bintray target repository package. Package can be optionally created on
	 * demand before deployment via {@link #performEnsure}.
	 */
	@Parameter(property = "bintray.packageName", defaultValue = "${project.artifactId}")
	private String packageName;

	/**
	 * Bintray rest-api-user for authentication. When missing, uses
	 * {@link #serverId}: {server/username} from settings.xml.
	 */
	@Parameter(property = "bintray.username")
	private String username;

	/**
	 * Bintray rest-api-token for authentication. When missing, uses
	 * {@link #serverId}: {server/password} from settings.xml.
	 */
	@Parameter(property = "bintray.password")
	private String password;

	/**
	 * Server id for credentials lookup via {@link serverId}: { server/username,
	 * server/password } in maven settings.xml.
	 */
	@Parameter(property = "bintray.serverId", defaultValue = "distro-bintray")
	private String serverId;

	/**
	 * Bintray package create definition parameter: version control system url.
	 */
	@Parameter(property = "bintray.packageVcsUrl", defaultValue = "${project.url}")
	private String packageVcsUrl;

	/**
	 * Bintray package create definition parameter: licenses list to attach to the
	 * target package.
	 */
	@Parameter(property = "bintray.packageLicenses", defaultValue = "Apache-2.0")
	private String[] packageLicenses;

	/**
	 * Deploy step 1: optionally remove target bintray package with all versions and
	 * files.
	 */
	@Parameter(property = "bintray.performDestroy", defaultValue = "false")
	private boolean performDestroy;

	/**
	 * Deploy step 2: optionally create target bintray package before deployment.
	 * Use create parameters: {@link #packageName}, {@link #packageVcsUrl},
	 * {@link #packageLicenses}
	 */
	@Parameter(property = "bintray.performEnsure", defaultValue = "true")
	private boolean performEnsure;

	/**
	 * Deploy step 3: actually do invoke artifact deployment to the target
	 * repository.
	 */
	@Parameter(property = "bintray.performDeploy", defaultValue = "true")
	private boolean performDeploy;

	/**
	 * Deploy step 4: optionally mark deployment artifact as "published for bintray
	 * consumption" after the deployment.
	 */
	@Parameter(property = "bintray.performPublish", defaultValue = "true")
	private boolean performPublish;

	/**
	 * Deploy step 5: optionally remove previous versions with files from target
	 * bintray package after the deployment. Preserve select versions via
	 * {@link #preserveRegex}.
	 */
	@Parameter(property = "bintray.performCleanup", defaultValue = "true")
	private boolean performCleanup;

	/**
	 * Deploy behaviour: during deployment cleanup, preserve versions with the
	 * version description matching given java regular expression.
	 */
	@Parameter(property = "bintray.preserveRegex", defaultValue = "(PRESERVE)")
	private String preserveRegex;

	/**
	 * Optionally skip all steps of the deployment execution.
	 */
	@Parameter(property = "maven.deploy.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * Parameter used to control how many times a failed deployment will be retried
	 * before giving up and failing. If a value outside the range 1-10 is specified
	 * it will be pulled to the nearest value within the range 1-10.
	 */
	@Parameter(property = "retryFailedDeploymentCount", defaultValue = "1")
	private int retryFailedDeploymentCount;

	/**
	 * Maven project providing deployment artifacts.
	 */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	/**
	 * Expose server credentials from settings.xml.
	 */
	@Parameter(defaultValue = "${settings}", required = true, readonly = true)
	private Settings settings;

	/**
	 * Maven session during execution.
	 */
	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	private MavenSession session;

	/**
	 * Repository materialization factory.
	 */
	@Component
	private ArtifactRepositoryFactory repositoryFactory;

	/**
	 * Repository layout definitions.
	 */
	@Component(role = ArtifactRepositoryLayout.class)
	private Map<String, ArtifactRepositoryLayout> repositoryLayouts;

	/**
	 * Location of deployment artifact.
	 */
	@Parameter(property = "project.build.finalName", required = false, readonly = true)
	private String finalName;

	/**
	 * Location of deployment artifact.
	 */
	@Parameter(property = "project.build.directory", required = false, readonly = true)
	private String buildDirectory;

	/**
	 * Bintray rest api data format.
	 */
	MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	/**
	 * Provide bintray rest api authentication from
	 * {@link #username}/{@link #password} with fallback to the {@link #serverId}.
	 */
	Authenticator authenticator = new Authenticator() {
		@Override
		public Request authenticate(Route route, Response response) throws IOException {
			String username = DeployMojo.this.username;
			String password = DeployMojo.this.password;
			if (username == null || password == null) {
				Server server = settings.getServer(serverId);
				if (server != null) {
					username = server.getUsername();
					password = server.getPassword();
				}
			}
			String credentials = Credentials.basic(username, password);
			return response.request().newBuilder().header("Authorization", credentials).build();
		}
	};

	/**
	 * Bintray rest api client.
	 */
	OkHttpClient client = new OkHttpClient.Builder().authenticator(authenticator).build();

	/**
	 * Maven upload url
	 * <p>
	 * https://bintray.com/docs/api/#_maven_upload
	 */
	String urlMavenDeploy() {
		// PUT /maven/:subject/:repo/:package/:file_path[;publish=0/1]
		return restApiUrl + "/maven/" + subject + "/" + mavenRepo + "/" + packageName;
	}

	/**
	 * Content publish url
	 * <p>
	 * https://bintray.com/docs/api/#_publish_discard_uploaded_content
	 */
	String urlContentPublish() {
		// POST /content/:subject/:repo/:package/:version/publish
		String version = project.getVersion();
		return restApiUrl + "/content/" + subject + "/" + mavenRepo + "/" + packageName + "/" + version + "/publish";
	}

	/**
	 * Package descriptor url
	 * <p>
	 * https://bintray.com/docs/api/#_get_package
	 */
	String urlPackageGet() {
		// GET /packages/:subject/:repo/:package[?attribute_values=1]
		return restApiUrl + "/packages/" + subject + "/" + mavenRepo + "/" + packageName;
	}

	/**
	 * Package create url
	 * <p>
	 * https://bintray.com/docs/api/#_create_package
	 */
	String urlPackageCreate() {
		// POST /packages/:subject/:repo
		return restApiUrl + "/packages/" + subject + "/" + mavenRepo;
	}

	/**
	 * Package delete url
	 * <p>
	 * https://bintray.com/docs/api/#url_delete_package
	 */
	String urlPackageDelete() {
		// DELETE /packages/:subject/:repo/:package
		return restApiUrl + "/packages/" + subject + "/" + mavenRepo + "/" + packageName;
	}

	/**
	 * Version descriptor url
	 * <p>
	 * https://bintray.com/docs/api/#_get_version
	 */
	String urlVersionGet(String version) {
		// GET /packages/:subject/:repo/:package/versions/:version[?attribute_values=1]
		return restApiUrl + "/packages/" + subject + "/" + mavenRepo + "/" + packageName + "/versions/" + version;
	}

	/**
	 * Version delete url
	 * <p>
	 * https://bintray.com/docs/api/#url_delete_version
	 */
	String urlVersionDelete(String version) {
		// DELETE /packages/:subject/:repo/:package/versions/:version
		return restApiUrl + "/packages/" + subject + "/" + mavenRepo + "/" + packageName + "/versions/" + version;
	}

	/**
	 * Verify bintray target package is present.
	 */
	boolean hasPackage() throws Exception {
		String url = urlPackageGet();
		Request request = new Request.Builder().url(url).build();
		Response response = client.newCall(request).execute();
		return response.code() == 200;
	}

	/**
	 * Render rest api response message.
	 */
	String render(String title, Response response) throws Exception {
		return title + " code=" + response.code() + " body=" + response.body().string();
	}

	/**
	 * Fetch bintray target package description.
	 */
	JSONObject packageGet() throws Exception {
		String url = urlPackageGet();
		Request request = new Request.Builder().url(url).build();
		Response response = client.newCall(request).execute();
		if (response.code() != 200) {
			getLog().error(render("Package fetch error", response));
			throw new RuntimeException();
		}
		String data = response.body().string();
		JSONObject json = new JSONObject(data);
		return json;
	}

	/**
	 * Create bintray target package entry with required meta data.
	 */
	void packageCreate() throws Exception {
		String url = urlPackageCreate();
		String json = new JSONObject() //
				.put("name", packageName) //
				.put("vcs_url", packageVcsUrl) //
				.put("licenses", packageLicenses) //
				.toString();
		RequestBody body = RequestBody.create(JSON, json);
		Request request = new Request.Builder().url(url).post(body).build();
		Response response = client.newCall(request).execute();
		if (response.code() != 201) {
			getLog().error(render("Package create error", response));
			throw new RuntimeException();
		}
	}

	/**
	 * Delete bintray target package with all versions of artifacts.
	 */
	void packageDelete() throws Exception {
		String url = urlPackageGet();
		Request request = new Request.Builder().url(url).build();
		Response response = client.newCall(request).execute();
		if (response.code() != 200) {
			getLog().error(render("Package delete error", response));
			throw new RuntimeException();
		}
	}

	/**
	 * Fetch bintray package version description.
	 */
	JSONObject versionGet(String version) throws Exception {
		String url = urlVersionGet(version);
		Request request = new Request.Builder().url(url).get().build();
		Response response = client.newCall(request).execute();
		if (response.code() != 200) {
			getLog().error(render("Version fetch error", response));
			throw new RuntimeException();
		}
		String data = response.body().string();
		JSONObject json = new JSONObject(data);
		return json;
	}

	/**
	 * Delete bintray package version description with attached artifacts.
	 */
	void versionDelete(String version) throws Exception {
		String url = urlVersionDelete(version);
		Request request = new Request.Builder().url(url).delete().build();
		Response response = client.newCall(request).execute();
		if (response.code() != 200) {
			getLog().error(render("Version delete error", response));
			throw new RuntimeException();
		}
	}

	/**
	 * Verify if version description contains magic regex.
	 */
	boolean hasPreserve(String version) throws Exception {
		JSONObject versionJson = versionGet(version);
		String key = "desc";
		if (versionJson.has(key)) {
			// Flatten content.
			String description = versionJson.get(key).toString();
			if (description.matches(preserveRegex)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove previous versions of artifacts from repository.
	 */
	void contentCleanup() throws Exception {
		getLog().info("Cleaning package content: " + packageName);
		JSONObject packageJson = packageGet();
		String latest = packageJson.optString("latest_version", "invalid_version");
		JSONArray versionList = packageJson.getJSONArray("versions");
		versionList.forEach((entry) -> {
			try {
				String version = entry.toString();
				if (!latest.equals(version)) {
					if (hasPreserve(version)) {
						getLog().info("Keeping version: " + version);
					} else {
						getLog().info("Erasing version: " + version);
						versionDelete(version);
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Mark deployed artifact as "published" for bintray consumption.
	 */
	void contentPublish() throws Exception {
		getLog().info("Publishing package content: " + packageName);
		String url = urlContentPublish();
		String json = "{}";
		RequestBody body = RequestBody.create(JSON, json);
		Request request = new Request.Builder().url(url).post(body).build();
		Response response = client.newCall(request).execute();
		if (response.code() != 200) {
			getLog().error(render("Publish content error", response));
			throw new RuntimeException();
		}
	}

	/**
	 * Invoke standard maven deployment process with custom bintray maven
	 * repository.
	 */
	void executeDeploy() throws Exception {

		getLog().info("Deploying package: " + packageName);

		// Create list of artifacts to be deployed.
		List<Artifact> artifactList = new ArrayList<>();
		artifactList.add(project.getArtifact());
		artifactList.addAll(project.getAttachedArtifacts());

		String url = urlMavenDeploy();

		// Create artifact repository.
		ArtifactRepository repository = repositoryFactory.createDeploymentArtifactRepository(serverId, url,
				repositoryLayouts.get("default"), false);

		// Set authentication if user and key are passed to properties.
		if (username != null && password != null) {
			repository.setAuthentication(new Authentication(username, password));
		}

		// Deploy artifacts.
		for (Artifact a : artifactList) {

			Path deployFilePath = a.getFile() != null ? a.getFile().toPath() : null;

			if (deployFilePath == null || !Files.exists(deployFilePath)) {
				if (deployFilePath != null) {
					getLog().debug("Artifact " + a + " doesn't exist in " + deployFilePath);
				}
				deployFilePath = Paths.get(buildDirectory, finalName + "." + a.getType());
			}

			if (!Files.exists(deployFilePath) && a.getType().equals("pom")) {
				getLog().debug("Artifact " + a + " doesn't exist in " + deployFilePath);
				deployFilePath = Paths.get(buildDirectory).getParent().resolve("pom.xml");
			}

			if (Files.exists(deployFilePath)) {
				deploy(deployFilePath.toFile(), a, repository, getLocalRepository(), retryFailedDeploymentCount);
			} else {
				getLog().debug("Artifact " + a + " doesn't exist in " + deployFilePath);
				getLog().warn("Cannot deploy artifact " + a + ", no file to deploy");
			}

		}

	}

	/**
	 * Delete package before deployment.
	 */
	void destroyPackage() throws Exception {
		if (hasPackage()) {
			getLog().info("Bintray package delete: ..." + packageName);
			packageDelete();
		} else {
			getLog().info("Bintray package is missing: " + packageName);
		}
	}

	/**
	 * Create target bintray package on demand.
	 */
	void ensurePackage() throws Exception {
		if (hasPackage()) {
			getLog().info("Bintray package is present: " + packageName);
		} else {
			getLog().info("Bintray package create ...: " + packageName);
			packageCreate();
		}
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skip plugin execution.");
			return;
		}
		try {
			if (performDestroy) {
				destroyPackage();
			}
			if (performEnsure) {
				ensurePackage();
			}
			if (performDeploy) {
				executeDeploy();
			}
			if (performPublish) {
				contentPublish();
			}
			if (performCleanup) {
				contentCleanup();
			}
		} catch (Throwable e) {
			throw new MojoFailureException("Deploy error", e);
		}
	}
}