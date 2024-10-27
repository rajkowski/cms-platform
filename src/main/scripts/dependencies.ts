// A script to copy updated dependencies into the web application
import fs from 'fs';
import path from 'path';
import packages from '../../../package.json';
import web from '../../../web-packages.json';

// web.json record
type WebDependency = {
  name?: string;
  paths: string[];
};

// the combined package.json and web.json record
type WebResource = {
  sourceDir: string;
  targetDir: string;
  version: string;
  paths: string[];
}

// For each web dependency, find the matching package dependency
const dependencies = web.dependencies;
Object.entries(dependencies).forEach(([key, value]) => {
  // The name of the package dependency is the key
  const resourceId = key as string;
  const webDependency = value as WebDependency;
  // Find and use the source package dependency
  const packageDependencies = Object.entries(packages.dependencies);
  const packageDependency = packageDependencies.find(([key]) => key === resourceId);
  if (packageDependency) {
    const version = packageDependency[1];
    let webResource = {
      "sourceDir": resourceId,
      "targetDir": `${webDependency.name || resourceId}-${version}`,
      "version": version,
      "paths": webDependency.paths
    }
    // Upgrade the packages in the web application
    try {
      console.log('Updating: ' + resourceId + '==' + version);
      copyWebResource(webResource);
      deleteOldWebResources(webResource);
    } catch (error) {
      console.error('Error with package dependency ' + resourceId + ': ' + error);
    }
  }
});

function copyWebResource(webResource: WebResource) {
  // Make sure the source has paths defined
  if (!webResource.paths.length) {
    throw new Error("Could not copy " + webResource.sourceDir);
  }

  // Determine the destination
  const webAppPath = path.join(process.cwd(), 'src', 'main', 'webapp', 'javascript');
  if (!isDirectory(webAppPath)) {
    console.log('Web application directory not found: ' + webAppPath);
    throw new Error('Web application directory not found: ' + webAppPath);
  }

  // Determine the target path
  const targetPath = path.join(webAppPath, webResource.targetDir);
  if (!exists(targetPath)) {
    // Create the target directory. if it does not exist
    fs.mkdirSync(targetPath);
    console.log('  Creating directory: ' + targetPath);
  }

  // Copy the directory contents and named files
  webResource.paths.forEach((packagePath) => {
    if (isDirectory(packagePath)) {
      // Will copy the directory
      console.log('  Copying directory ' + packagePath + ' to ' + targetPath);
      copyDirectory(packagePath, targetPath);
    } else if (isFile(packagePath)) {
      // Will copy the file
      const targetFile = path.join(targetPath, path.basename(packagePath));
      console.log('  Copying file ' + packagePath + ' to ' + targetFile);
      fs.copyFileSync(packagePath, targetFile);
    }
  });
}

function deleteOldWebResources(webResource: WebResource) {
  // console.log('  Deleting old directories of: ' + webResource.targetDir);
}

function isFile(path: string) {
  try {
    return fs.statSync(path).isFile();
  }
  catch (err) {
    return false;
  }
}

function isDirectory(path: string) {
  try {
    return fs.statSync(path).isDirectory();
  }
  catch (err) {
    return false;
  }
}

function exists(path: string) {
  return fs.existsSync(path);
}

// Copies the contents of one directory to another directory
function copyDirectory(source: string, destination: string) {
  fs.mkdirSync(destination, { recursive: true });

  fs.readdirSync(source, { withFileTypes: true }).forEach((entry) => {
    let sourcePath = path.join(source, entry.name);
    let destinationPath = path.join(destination, entry.name);

    entry.isDirectory()
      ? copyDirectory(sourcePath, destinationPath)
      : fs.copyFileSync(sourcePath, destinationPath);
  });
}
