/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rocks.stalin.android.app.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for the {@link PermissionHelper} class. Exercises the helper methods that
 * do adding and removing from the static list of missing permissions
 */
@RunWith(JUnit4.class)
public class PermissionHelperTest {

    @Test
    public void testAddingMissingPermission() throws Exception {
        PermissionHelper.addMissingPermission("TEST_MISSING_PERMISSION");
        assertArrayEquals(new String[]{"TEST_MISSING_PERMISSION"}, PermissionHelper.getMissingPermissions().toArray(new String[1]));
        PermissionHelper.removeAllMissingPermissions();
    }

    @Test
    public void testRemovingMissingPermission() throws Exception {
        PermissionHelper.addMissingPermission("TEST_MISSING_PERMISSION");
        PermissionHelper.addMissingPermission("TEST_MEME_PERMISSION");
        assertArrayEquals(new String[]{"TEST_MISSING_PERMISSION", "TEST_MEME_PERMISSION"}, PermissionHelper.getMissingPermissions().toArray(new String[2]));
        PermissionHelper.removeMissingPermission("TEST_MISSING_PERMISSION");
        assertArrayEquals(new String[]{"TEST_MEME_PERMISSION"}, PermissionHelper.getMissingPermissions().toArray(new String[1]));
        PermissionHelper.removeAllMissingPermissions();
    }

    @Test
    public void testRemoveAllMissingPermission() throws Exception {
        PermissionHelper.addMissingPermission("TEST_MISSING_PERMISSION_1");
        PermissionHelper.addMissingPermission("TEST_MISSING_PERMISSION_2");
        PermissionHelper.addMissingPermission("TEST_MISSING_PERMISSION_3");
        PermissionHelper.addMissingPermission("TEST_MISSING_PERMISSION_4");
        PermissionHelper.addMissingPermission("TEST_MISSING_PERMISSION_5");
        assertEquals(PermissionHelper.getMissingPermissions().size(), 5);
        PermissionHelper.removeAllMissingPermissions();
        assertEquals(PermissionHelper.getMissingPermissions().size(), 0);
    }

}
