import fs from 'fs';
import path from 'path';
import packages from '../../../package.json';
import web from '../webapp/WEB-INF/web-packages.json';

// web.json record
type WebDependency = {
  package?: string;
  version?: string;
  paths?: string[];
};

// the combined package.json and web.json record
type WebResource = {
  sourceDir: string;
  targetName: string;
  targetDir: string;
  version: string;
  paths?: string[];
}

// Determine the destination
const webAppPath = path.join(process.cwd(), 'src', 'main', 'webapp', 'javascript');
if (!isDirectory(webAppPath)) {
  console.log('Web application directory not found: ' + webAppPath);
  throw new Error('Web application directory not found: ' + webAppPath);
}

// For each web dependency, find the matching package dependency
const dependencies = web.dependencies;
Object.entries(dependencies).forEach(([key, value]) => {
  // The name of the package dependency is the key
  const resourceId = key as string;
  const webDependency = value as WebDependency;
  // Find and use the source package dependency
  const packageId = webDependency.package || resourceId;
  const packageDependencies = Object.entries(packages.dependencies);
  const packageDependency = packageDependencies.find(([key]) => key === packageId);
  if (packageDependency) {
    const version = packageDependency[1];
    let webResource = {
      "sourceDir": `${packageId}`,
      "targetName": `${resourceId}`,
      "targetDir": `${resourceId}-${version}`,
      "version": version,
      "paths": webDependency.paths
    }
    // Upgrade the packages in the web application
    try {
      console.log('Resource: ' + resourceId + '==' + version);
      if (webResource.paths) {
        copyWebResource(webAppPath, webResource);
        deleteOldWebResources(webAppPath, webResource);
        webDependency.version = version;
      } else {
        console.log('  Skipped.');
      }
    } catch (error) {
      console.error('Error with package dependency ' + resourceId + ': ' + error);
    }
  }

  // Save the file indicating the installed version
  const file = path.join('src', 'main', 'webapp', 'WEB-INF', 'web-packages.json')
  fs.writeFileSync(file, JSON.stringify(web, null, 2));
});

function copyWebResource(webAppPath: string, webResource: WebResource) {
  // Make sure the source has paths defined
  if (!webResource.paths?.length) {
    throw new Error("Could not copy " + webResource.sourceDir);
  }

  // Determine the target path
  const targetPath = path.join(webAppPath, webResource.targetDir);
  if (!exists(targetPath)) {
    // Create the target directory. if it does not exist
    fs.mkdirSync(targetPath);
    console.log('  Creating directory: ' + targetPath);
  }

  // Copy the directory contents and named files
  webResource.paths?.forEach((packagePath) => {
    if (isDirectory(packagePath) && packagePath.endsWith('/*')) {
      // Will copy the contents of the directory
      console.log('  Copying directory contents ' + packagePath + ' to ' + targetPath);
      copyDirectory(packagePath, targetPath);
    } else if (isDirectory(packagePath)) {
      // Will copy the directory
      const newTargetPath = path.join(targetPath, path.basename(packagePath));
      console.log('  Copying directory ' + packagePath + ' to ' + newTargetPath);
      if (!exists(newTargetPath)) {
        // Create the target directory. if it does not exist
        fs.mkdirSync(newTargetPath);
      }
      copyDirectory(packagePath, newTargetPath);
    } else if (isFile(packagePath)) {
      // Will copy the file
      const targetFile = path.join(targetPath, path.basename(packagePath));
      console.log('  Copying file ' + packagePath + ' to ' + targetFile);
      fs.copyFileSync(packagePath, targetFile);
    }
  });
}

function deleteOldWebResources(webAppPath: string, webResource: WebResource) {
  fs.readdirSync(webAppPath, { withFileTypes: true }).forEach((entry) => {
    const installedBasename = path.basename(entry.name);
    const resourceBasename = path.basename(webResource.targetDir);
    // Look for directories to delete which have the exact same name, but different version
    if (entry.isDirectory() &&
      installedBasename !== resourceBasename &&
      installedBasename.substring(installedBasename.lastIndexOf('-') + 1) !== 'plugins' &&
      installedBasename.startsWith(webResource.targetName) &&
      installedBasename.substring(0, installedBasename.lastIndexOf('-')) === installedBasename.substring(0, resourceBasename.lastIndexOf('-'))) {
      // Remove previous versions
      const directoryToDelete = path.join(webAppPath, entry.name);
      console.log('  *Deleting directory... ' + directoryToDelete);
      fs.rmSync(directoryToDelete, { recursive: true });
    }
  });
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
