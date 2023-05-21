#include <cassert>

#include "tilelogger_TileLogger.h"
#include "TileLogger.h"

static TileLogger g_map_history;

JNIEXPORT jlong JNICALL Java_tilelogger_TileLogger_reset (JNIEnv*, jclass, jshort width, jshort height) {
    return g_map_history.Reset({width, height});
}

JNIEXPORT jshort JNICALL Java_tilelogger_TileLogger_duration (JNIEnv*, jclass) {
    return g_map_history.Duration();
}

JNIEXPORT void JNICALL Java_tilelogger_TileLogger_onAction (JNIEnv* env, jclass,
    jshort x, jshort y, jstring juuid, jshort team, jshort block, jshort rotation, jshort config_type, jint config) {

    std::string uuid = env->GetStringUTFChars(juuid, NULL);
    g_map_history.Record({x, y}, uuid, team, block, rotation, config_type, config);
}

JNIEXPORT void JNICALL Java_tilelogger_TileLogger_onAction2 (JNIEnv* env, jclass, 
    jshort x, jshort y, jstring juuid, jshort team, jshort block, jshort rotation, jshort config_type, jbyteArray jconfig) {
        
    std::string uuid = env->GetStringUTFChars(juuid, NULL);
    std::byte* jconfig_ptr = reinterpret_cast<std::byte*>(env->GetByteArrayElements(jconfig, NULL));
    jsize jconfig_len = env->GetArrayLength(jconfig);
    g_map_history.Record({x, y}, uuid, team, block, rotation, config_type, ConfigData(jconfig_ptr, jconfig_ptr + jconfig_len));
}

jobjectArray MarhalTileStateArray(JNIEnv* env, const std::vector<TileState> vec) {
    jclass state_class = env->FindClass("tilelogger/TileState"); assert(state_class != nullptr);
    jfieldID x_field = env->GetFieldID(state_class, "x", "S"); assert(x_field != nullptr);
    jfieldID y_field = env->GetFieldID(state_class, "y", "S"); assert(y_field != nullptr);
    jfieldID uuid_field = env->GetFieldID(state_class, "uuid", "Ljava/lang/String;"); assert(uuid_field != nullptr);
    jfieldID team_field = env->GetFieldID(state_class, "team", "B"); assert(team_field != nullptr);
    jfieldID time_field = env->GetFieldID(state_class, "time", "S"); assert(time_field != nullptr);
    jfieldID valid_field = env->GetFieldID(state_class, "valid", "Z"); assert(valid_field != nullptr);
    jfieldID block_field = env->GetFieldID(state_class, "block", "S"); assert(block_field != nullptr);
    jfieldID rotation_field = env->GetFieldID(state_class, "rotation", "S"); assert(rotation_field != nullptr);
    jfieldID config_type_field = env->GetFieldID(state_class, "config_type", "S"); assert(config_type_field != nullptr);
    jfieldID config_field = env->GetFieldID(state_class, "config", "Ljava/lang/Object;"); assert(config_field != nullptr);
    
    jclass integer_class = env->FindClass("Ljava/lang/Integer;"); assert(integer_class != nullptr);
    jmethodID integer_class_ctr = env->GetMethodID(integer_class, "<init>", "(I)V"); assert(integer_class_ctr != nullptr);

    jobjectArray object_array_j = env->NewObjectArray(static_cast<jsize>(vec.size()), state_class, nullptr); assert(object_array_j != nullptr);
    for (int i = 0; i < vec.size(); i++) {
        jobject state_j = env->AllocObject(state_class); assert(state_j != nullptr);
        env->SetShortField(state_j, x_field, vec[i].pos.x);
        env->SetShortField(state_j, y_field, vec[i].pos.y);
        env->SetObjectField(state_j, uuid_field, env->NewStringUTF(g_map_history.GetPlayer(vec[i].player).c_str()));
        env->SetByteField(state_j, team_field, vec[i].team);
        env->SetShortField(state_j, time_field, vec[i].time);
        env->SetBooleanField(state_j, valid_field, vec[i].valid);
        env->SetShortField(state_j, block_field, vec[i].block);
        env->SetShortField(state_j, rotation_field, vec[i].rotation);
        env->SetShortField(state_j, config_type_field, vec[i].config_type);
        if (vec[i].config_type >= 5) {
            const ConfigData& config_data = g_map_history.GetConfig(vec[i].config);
            jbyteArray config_data_j = env->NewByteArray(static_cast<jsize>(config_data.size()));
            env->SetByteArrayRegion(config_data_j, 0, static_cast<jsize>(config_data.size()), reinterpret_cast<const jbyte*>(config_data.data()));
            env->SetObjectField(state_j, config_field, config_data_j);
        }
        else {
            env->SetObjectField(state_j, config_field, env->NewObject(integer_class, integer_class_ctr, vec[i].config));
        }
        env->SetObjectArrayElement(object_array_j, i, state_j);
    }
    
    return object_array_j;
}

JNIEXPORT jobjectArray JNICALL Java_tilelogger_TileLogger_getHistory (JNIEnv* env, jclass,
    jshort x1, jshort y1, jshort x2, jshort y2, jstring juuid, jint teams, jint time, jlong size) {

    std::string uuid = env->GetStringUTFChars(juuid, NULL);
    return MarhalTileStateArray(env, g_map_history.GetHistory({x1, y1, x2, y2}, uuid, teams, time, size));
}

JNIEXPORT jobjectArray JNICALL Java_tilelogger_TileLogger_rollback (JNIEnv* env, jclass,
    jshort x1, jshort y1, jshort x2, jshort y2, jstring juuid, jint teams, jint time, jint flags) {

    std::string uuid = env->GetStringUTFChars(juuid, NULL);
    return MarhalTileStateArray(env, g_map_history.Rollback({x1, y1, x2, y2}, uuid, teams, time, static_cast<HistoryStack::RollbackFlags>(flags)));
}

JNIEXPORT jlong JNICALL Java_tilelogger_TileLogger_memoryUsage (JNIEnv*, jclass, jlong id) {
    return g_map_history.MemoryUsage(id);
}

JNIEXPORT jstring JNICALL Java_tilelogger_TileLogger_getBuildString (JNIEnv* env, jclass) {
    return env->NewStringUTF(__DATE__ " " __TIME__);
}